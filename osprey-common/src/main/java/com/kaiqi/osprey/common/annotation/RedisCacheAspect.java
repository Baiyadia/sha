package com.kaiqi.osprey.common.annotation;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wangs
 * @title: CachableAspect
 * @package com.kaiqi.osprey.common.annotation
 * @description: TODO
 * @date 2019-06-05 17:26
 */
@Slf4j
@Aspect
@Component
@Order(3)
public class RedisCacheAspect {

    @Autowired
    private StringRedisTemplate template;

    private static final Map<String, LocalCache> memValueMap = new ConcurrentHashMap();

    public RedisCacheAspect() {
    }

    private Object getValue(RedisCache redisCache, String key, Type returnType) {
        if (RedisCache.CacheType.REDIS.equals(redisCache.valuePosition())) {
            return JSONObject.parseObject(template.opsForValue().get(key), returnType);
        } else if (RedisCache.CacheType.LOCAL.equals(redisCache.valuePosition())) {
            LocalCache localCache = memValueMap.get(key);
            if (localCache == null) {
                return null;
            } else if (localCache.getExpireTime() > 0L && System.currentTimeMillis() > localCache.getExpireTime()) {
                memValueMap.remove(key);
                return null;
            } else {
                return localCache.getValue();
            }
        } else {
            return null;
        }
    }

    private void setValue(RedisCache redisCache, String key, Object value) {
        if (RedisCache.CacheType.REDIS.equals(redisCache.valuePosition())) {
            if (redisCache.timeout() > 0) {
                template.opsForValue().set(key, JSONObject.toJSONString(value), redisCache.timeout(), redisCache.unit());
            } else {
                template.opsForValue().set(key, JSONObject.toJSONString(value));
            }
        } else if (RedisCache.CacheType.LOCAL.equals(redisCache.valuePosition())) {
            LocalCache cache = new LocalCache();
            cache.setValue(value);
            if (redisCache.timeout() > 0) {
                cache.setExpireTime(redisCache.unit().toMillis(redisCache.timeout()) + System.currentTimeMillis());
            }
            memValueMap.put(key, cache);
        }
    }

    @Around("@annotation(com.kaiqi.osprey.common.annotation.RedisCache)")
    public Object intercept(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> clazz = joinPoint.getTarget().getClass();

        Method method = clazz.getDeclaredMethod(signature.getMethod().getName(), signature.getMethod().getParameterTypes());

        RedisCache annotation = method.getAnnotation(RedisCache.class);

        Object[] args = joinPoint.getArgs();
        String key = this.getKey(annotation, args);

        Object result = null;
        if (!StringUtils.isEmpty(key)) {
            try {
                result = this.getValue(annotation, key, method.getGenericReturnType());
                if (result != null) {
                    if (RedisCacheAspect.NullValue.getInstance().equals(result)) {
                        return null;
                    }
                    return result;
                }
            } catch (Exception e) {
                log.error("redis cache拦截器获取缓存出错 key={}", key, e);
            }
        }

        result = joinPoint.proceed();

        if (!this.shouldStore(key, args, result, annotation.excludeCondition())) {
            return result;
        } else {
            if (result == null) {
                result = RedisCacheAspect.NullValue.getInstance();
            }
            try {
                this.setValue(annotation, key, result);
            } catch (Exception e) {
                log.error("redis cache拦截器放入缓存出错 key={} value={}", key, JSONObject.toJSONString(result), e);
            }
            return RedisCacheAspect.NullValue.getInstance().equals(result) ? null : result;
        }
    }

    private boolean shouldStore(String key, Object[] args, Object result, String excludeCondition) {
        if (StringUtils.isEmpty(key)) {
            return false;
        } else if (StringUtils.isEmpty(excludeCondition)) {
            return true;
        } else {
            ExpressionParser parser = new SpelExpressionParser();
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("args", this.getArrangedArgs(args));
            context.setVariable("key", key);
            context.setVariable("result", result);
            return !"true".equalsIgnoreCase(parser.parseExpression(excludeCondition).getValue(context, String.class));
        }
    }

    private String getKey(RedisCache redisCache, Object[] args) {
        ExpressionParser parser = new SpelExpressionParser();
        if (args != null && args.length > 0) {
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("args", this.getArrangedArgs(args));
            return parser.parseExpression(redisCache.key()).getValue(context, String.class);
        } else {
            Expression expression = parser.parseExpression(redisCache.key());
            return expression.getValue(String.class);
        }
    }

    private Object[] getArrangedArgs(Object[] args) {
        if (args == null) {
            return args;
        } else {
            for (int i = 0; i < args.length; ++i) {
                if (ObjectUtils.isEmpty(args[i])) {
                    args[i] = "";
                }
            }
            return args;
        }
    }

    public static class NullValue implements Serializable {

        private static final RedisCacheAspect.NullValue INSTANCE = new RedisCacheAspect.NullValue();

        public NullValue() {
        }

        public static RedisCacheAspect.NullValue getInstance() {
            return INSTANCE;
        }

        public boolean equals(Object obj) {
            return obj == null ? false : obj instanceof RedisCacheAspect.NullValue;
        }
    }
}
