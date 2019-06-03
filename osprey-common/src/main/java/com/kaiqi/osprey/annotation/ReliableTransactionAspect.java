package com.kaiqi.osprey.annotation;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(3)
public class ReliableTransactionAspect {

    //    @Before("@aspect(ReliableTransaction)")
//    public void interceptB(JoinPoint joinPoint) {
//        System.out.println("@Before");
//    }
//
//    @After("execution(* com.kaiqi.osprey.service..*(..))")
//    public void interceptC(JoinPoint joinPoint) {
//        System.out.println("@After");
//    }
//    @AfterReturning("@aspect(TestValidate)")
//    public void interceptD(JoinPoint joinPoint) throws Throwable {
//        System.out.println("@AfterReturning");
//    }

    @Around("@annotation(com.kaiqi.osprey.annotation.ReliableTransaction)")
    public Object interceptA(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        TestValidate anno = signature.getMethod().getAnnotation(TestValidate.class);
        Object[] args = joinPoint.getArgs();

        System.out.println("@Around Before");
        Object result = joinPoint.proceed();
        System.out.println("@Around After");

        return result;
    }

}