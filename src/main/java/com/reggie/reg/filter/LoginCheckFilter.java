package com.reggie.reg.filter;

import com.alibaba.fastjson.JSON;
import com.reggie.reg.common.R;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

/**
 * 设置过滤器，检查用户是否完成了登录
 *
 */
//@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Autowired
    private HttpServletResponse response;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        //对其进行向下转型
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        log.info("拦截到请求：{}", request.getRequestURI());
        //1.获取本次请求的URI
        String requestURI = request.getRequestURI();
        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "backend/**",
                "/front/**"
        };
        //2.判断本次请求是否需要处理
        boolean check = check(urls, requestURI);
        if (check) {
            //如果不需要处理直接放行
            log.info("本次请求{}不需要处理" , requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        //4.判断登陆状态，如果已经登录，则直接放行
        if (request.getSession().getAttribute("employee") != null) {
            log.info("用户已登录，用户id为：{}", request.getSession().getAttribute("employee"));
            filterChain.doFilter(request, response);
            return;
        }

        //5.如果没有登录则返回登陆结果,通过输出流的方式向前端返回数据
        log.info("用户未登录");
        String notlogin = JSON.toJSONString(R.error("NOTLOGIN"));
        response.getWriter().write(notlogin);
        return;
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

    /**
     * 封装比较方法，路径匹配，检查本次请求是否需要放行
     *
     * @param requestURI 将比较用的url传入
     * @param urls       路径参照
     * @return
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }
}
