package com.reggie.reg.common;

/**
 * 自定义业务异常
 */
public class CustomException extends RuntimeException {
    public CustomException(String message) {
        // 调用父类的异常处理方法
        super(message);
    }
}
