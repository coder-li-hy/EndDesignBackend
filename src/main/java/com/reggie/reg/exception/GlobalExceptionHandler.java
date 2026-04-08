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
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)//捕获数据库异常
    public R<String> SQLexceptionHandler(Exception ex) {
        log.error(ex.getMessage());
        if (ex.getMessage().contains("Duplicate entry")) {
            String[] split = ex.getMessage().split(" ");
            String username = split[9].replace("'", "");
            String msg =username +"已存在";
            return R.error(msg);
        }
        return R.error("对不起，未知错误，操作失败，请联系管理员");
    }

    /**
     * 处理自定义异常
     * @param ex
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<String> DataRelateExceptionHandler(Exception ex) {
        log.error(ex.getMessage());
        return R.error(ex.getMessage());
    }
}
