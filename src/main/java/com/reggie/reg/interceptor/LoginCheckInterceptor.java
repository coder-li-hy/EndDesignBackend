package com.reggie.reg.interceptor;

import com.alibaba.fastjson.JSON;
import com.reggie.reg.common.BaseContext;
import com.reggie.reg.common.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;



@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {



    /**
     * 登录拦截方法，完善逻辑
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("拦截到请求{}", request.getRequestURI());
//        long id = Thread.currentThread().getId();
//        log.info("线程id:{}", id);
        //1判断登录状态
        //1.1获取本次请求的url
        StringBuffer url = request.getRequestURL();
        //1.2判断是否处于登陆状态，如果处于登陆状态，即session中有用户数据
        Long id = (Long)request.getSession().getAttribute("employee");
        if (id != null) {
            //设置当前线程本地变量
            BaseContext.setCurrentId(id);
            //直接放行
            return true;
        }
        // 判断移动端用户的登录状态
        if (request.getSession().getAttribute("user") != null) {

            //设置当前线程本地变量
            BaseContext.setCurrentId((Long)request.getSession().getAttribute("user"));
            //直接放行
            return true;
        }
        //2.如果没有登陆则返回登陆结果，如果已经登录，则直接放行
        String jsonResult = JSON.toJSONString(R.error("NOTLOGIN"));
        response.getWriter().write(jsonResult);
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("postHandle");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.info("afterCompletion");
    }
}
