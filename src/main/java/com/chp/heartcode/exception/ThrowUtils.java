package com.chp.heartcode.exception;

/**
 * @Author: CHP
 * @Description: 异常工具类
 */
public class ThrowUtils {

    /**
     * 抛出业务异常1
     *
     * @param condition        判断条件
     * @param runtimeException 业务异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 抛出业务异常2
     *
     * @param condition 判断条件
     * @param errorCode 业务异常
     */
    public static void throwIF(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 抛出业务异常3
     *
     * @param condition 判断条件
     * @param errorCode 业务异常
     * @param message   异常信息
     */
    public static void throwIF(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
