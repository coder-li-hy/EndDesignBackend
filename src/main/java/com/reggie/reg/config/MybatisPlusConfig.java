package com.reggie.reg.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置mp的分页插件
 */
@Configuration
public class MybatisPlusConfig {

    // 创建一个MybatisPlusInterceptor类型的bean
 @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建一个MybatisPlusInterceptor类型的实例
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        // 向mybatisPlusInterceptor中添加一个PaginationInnerInterceptor类型的拦截器
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // 返回mybatisPlusInterceptor实例
        return mybatisPlusInterceptor;
    }
}
