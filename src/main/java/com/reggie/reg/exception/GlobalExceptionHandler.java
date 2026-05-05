package com.reggie.reg.exception;

import com.reggie.reg.common.CustomException;
import com.reggie.reg.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 处理数据库异常的方法
     * @param ex 捕获到的异常对象
     * @return 返回一个R对象，包含错误信息
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)//捕获数据库异常
    public R<String> SQLexceptionHandler(Exception ex) {
        log.error(ex.getMessage()); // 记录错误日志
        if (ex.getMessage().contains("Duplicate entry")) { // 判断是否为重复条目异常
            String[] split = ex.getMessage().split(" "); // 分割错误消息字符串
            String username = split[9].replace("'", ""); // 获取用户名并去除单引号
            String msg =username +"已存在"; // 构造错误消息
            return R.error(msg); // 返回用户已存在的错误信息
        }
        return R.error("对不起，未知错误，操作失败，请联系管理员"); // 返回通用错误信息
    }


    /**
     * 处理自定义异常的异常处理器
     * 当系统中抛出CustomException类型的异常时，此方法会被自动调用
     *
     * @param ex 捕获到的异常对象，这里限定为CustomException类型
     * @return 返回一个R对象，其中包含错误信息，用于前端统一处理异常响应
     */
    @ExceptionHandler(CustomException.class)  // 标记此方法为处理CustomException类型异常的处理器
    public R<String> DataRelateExceptionHandler(Exception ex) {  // 方法名表明这是处理数据相关异常的方法
        log.error(ex.getMessage());  // 记录错误日志，方便问题追踪
        return R.error(ex.getMessage());  // 返回错误信息给前端
    }
}
