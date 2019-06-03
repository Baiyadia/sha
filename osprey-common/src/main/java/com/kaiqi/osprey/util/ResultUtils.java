package com.kaiqi.osprey.util;

import com.kaiqi.osprey.commons.ResponseResult;
import com.kaiqi.osprey.commons.enums.ErrorCodeEnum;
import com.kaiqi.osprey.exception.OspreyBizException;

/**
 * @author wangs
 * @title: ResultUtils
 * @package com.kaiqi.osprey.util
 * @description: TODO
 * @date 2019-06-01 22:15
 */
public class ResultUtils {

    public static ResponseResult success(Object data) {
        return new ResponseResult("success", 0, "", data);
    }

    public static ResponseResult failure() {
        return failure(ErrorCodeEnum.UNKNOWN_ERROR);
    }

    public static ResponseResult failure(OspreyBizException e) {
        return failure(e.getErrorCode());
    }

    public static ResponseResult failure(ErrorCodeEnum errorCodeEnum) {
        return new ResponseResult("fail", errorCodeEnum.getCode(), errorCodeEnum.getMsg(), null);
    }

}