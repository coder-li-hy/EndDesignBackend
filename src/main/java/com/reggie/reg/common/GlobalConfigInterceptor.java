package com.reggie.reg.common;// interceptor/GlobalConfigInterceptor.java

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.SystemConfig;
import com.reggie.reg.service.ISystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class GlobalConfigInterceptor implements HandlerInterceptor {

    private final ISystemConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        LambdaQueryWrapper<SystemConfig> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getConfigKey,"MAINTENANCE_MODE");
        String maintenance_mode=configService.getOne(queryWrapper).getConfigValue();
        LambdaQueryWrapper<SystemConfig> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey,"GLOBAL_SELECTION_SWITCH");
        String global_selection_switch=configService.getOne(wrapper).getConfigValue();

        // 系统维护模式校验（管理员除外 其余用户全部拦截）
        if ("1".equals(maintenance_mode)) {
            Integer userId = (Integer) request.getSession().getAttribute("sys_user");
            String role = (String) request.getSession().getAttribute("sys_user_role");

            // 管理员可访问所有接口，其他角色拦截
            if (!"ADMIN".equals(role)) {
                return writeJsonResponse(response, R.error("系统维护中，请稍后访问"));
            }
        }

        if ("0".equals(global_selection_switch)) {
            if (isSelectionRelatedUri(uri, method)) {
                return writeJsonResponse(response, R.error("选课功能已关闭，暂无法操作"));
            }
        }

        return true;  // 放行
    }

    /**
     * 判断是否为选课相关接口
     */
    private boolean isSelectionRelatedUri(String uri, String method) {
        // 只拦截修改类请求（POST/PUT/DELETE），GET 请求（查看）不拦截
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
            return false;
        }

        // 匹配选课相关路径
        return uri.contains("/student/courses") &&
                (uri.contains("/select") || uri.contains("/cancel"));
    }

    private boolean writeJsonResponse(HttpServletResponse response, R<?> result) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false;  // 拦截请求
    }
}