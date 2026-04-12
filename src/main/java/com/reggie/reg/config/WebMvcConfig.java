package com.reggie.reg.config;

import com.reggie.reg.common.JacksonObjectMapper;
import com.reggie.reg.interceptor.LoginCheckInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

/**
 * 在config包下创建配置类，放置项目配置文件
 */
//为了说明该类是一个配置类
@Slf4j
@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {
    // ⭐ 新增：读取上传路径配置（添加默认值 + 兼容 Windows 路径）
    @Value("${reggie.path:E:/EndDesign/backend3/src/main/resources/file/}")
    private String uploadPath;
    /**
     * 进行静态资源映射
     * 重写类中的方法，使放在resources中的静态资源能够被访问到
     *
     * @param registry
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ⭐ 映射 /uploads/** 到本地文件目录
        // 前端访问: http://your-domain/uploads/abc123.pptx
        // 实际读取: E:/EndDesign/backend3/src/main/resources/file/abc123.pptx
        log.info("配置文件上传路径: {}", uploadPath);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);

        //将前段的请求映射到后端页面的静态资源中
//        log.info("开始静态资源映射！");
//        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
//        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
//        log.info("静态资源映射成功！");
//
    }

    //注入请求拦截器
    @Autowired
    private LoginCheckInterceptor loginCheckInterceptor;

    /**
     * 添加注册请求拦截器，完善登录功能
     * 除了登录请求，其他一律拦截
     *
     * @param registry
     */
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(loginCheckInterceptor).addPathPatterns("/**").excludePathPatterns("/employee/login",
//                "/employee/logout",
//                //以下两个是静态资源的请求地址路径模式
//                "/backend/**",
//                "/front/**"
//                ,"/common/**"
//                ,"/user/sendMsg"// 移动端短信登录
//                ,"/user/login"// 移动端登录
//        );
    }

    /**
     * 扩展mvc框架的消息转换器
     *
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 创建消息转换器对象
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        // 设置对象转换器，底层使用Jackson将java对象转为json
        mappingJackson2HttpMessageConverter.setObjectMapper(new JacksonObjectMapper());

        // 将上面的消息转换器对象追加到mvc框架的转换器集合中
        // 添加时需要跟上index索引，转换器是有顺序的，放到后面则程序不会使用我们自己的转换器
        converters.add(0, mappingJackson2HttpMessageConverter);
    }
}
