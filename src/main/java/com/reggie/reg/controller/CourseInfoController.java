package com.reggie.reg.controller;


import com.reggie.reg.common.R;
import com.reggie.reg.dto.CourseDTO;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.impl.CourseInfoServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 课程信息表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
public class CourseInfoController {

    private final ICourseInfoService courseInfoService;
    /**
     * 创建课程（只有教师能访问）
     * 核心：后端从 Session 校验角色，不信任前端
     */
    @PostMapping("/teacher/courses")
    public R<CourseInfo> createCourse(HttpServletRequest request,
                                      @RequestBody CourseDTO dto) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return R.error("NOT_LOGIN");
        }
        // 检测传入时间是否合法
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            return R.error("课程时间不合法");
        }

        // ⭐ 从 Session 获取用户信息（安全）
        Integer userId = (Integer) session.getAttribute("sys_user");
        String role = (String) session.getAttribute("sys_user_role");

        // ⭐ 校验角色（关键！）
        if (!"TEACHER".equals(role)) {
            return R.error("无权访问");
        }

        // 数据整备
        // 将传入的课程信息丰富外键teacherId
        dto.setTeacherId(userId);
        // 设置当前人数为0

        // 业务逻辑：创建课程
        if(courseInfoService.save(dto)){
            return R.success(courseInfoService.getById(dto.getCourseId()));
        }else {
            return R.error("创建课程失败");
        }
    }

}
