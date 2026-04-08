package com.reggie.reg.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自定义元数据对象处理器,实现实体类公共字段的填充
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {
    // 将当前用户请求的bean对象注入
    @Autowired
    private HttpServletRequest request;

    /**
     * 插入操作，自动填充
     *
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        long id = Thread.currentThread().getId();
        log.info("线程id:{}", id);
        log.info("公共字段自动填充[insert]...,当前用户{}", BaseContext.getCurrentId());
        log.info(metaObject.toString());
        metaObject.setValue("createTime", LocalDateTime.now());
            metaObject.setValue("updateTime", LocalDateTime.now());
            //通过ThreadLocal获取当前登录用户信息
            metaObject.setValue("createUser", BaseContext.getCurrentId());
            metaObject.setValue("updateUser", BaseContext.getCurrentId());
            metaObject.setValue("isDeleted", 0);
//        metaObject.setValue("createUser",request.getSession().getAttribute("employee"));
//        metaObject.setValue("updateUser",request.getSession().getAttribute("employee"));

    }

    /**
     * 更新操作，自动填充
     *
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充[update]...");
        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("updateUser", BaseContext.getCurrentId());
        log.info(metaObject.toString());
    }
}
