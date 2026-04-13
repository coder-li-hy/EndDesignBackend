package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.dto.CourseDTO;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.impl.CourseInfoServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
     * 1. 分页获取教师课程列表
     * GET /api/teacher/courses?courseName=&status=&page=1&size=10
     */
    @GetMapping("/teacher/courses")
    public R<Page<CourseInfo>> listCourses(
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        // 1. 获取当前教师 ID
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        if (teacherId == null) {
            return R.error("未登录");
        }

        // 2. 构建查询条件
        LambdaQueryWrapper<CourseInfo> query = new LambdaQueryWrapper<>();
        query.eq(CourseInfo::getTeacherId, teacherId);  // ⭐ 只查自己的课
        query.like(StringUtils.isNotBlank(courseName), CourseInfo::getCourseName, courseName);
        query.eq(StringUtils.isNotBlank(status), CourseInfo::getStatus, status);
        query.orderByDesc(CourseInfo::getCourseId);

        return R.success(courseInfoService.page(new Page<>(page, size), query));
    }


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
        if (dto.getStartDate() == null || dto.getEndDate() == null||(dto.getStartDate().isAfter(dto.getEndDate()))) {
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
    /**
     * 3. 更新课程信息
     * PUT /api/teacher/courses/{courseId}
     */
    @PutMapping("/teacher/courses/{courseId}")
    public R<String> updateCourse(@PathVariable Integer courseId,
                                  @RequestBody CourseInfo dto,
                                  HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        CourseInfo course = courseInfoService.getById(courseId);
        if (course == null) return R.error("课程不存在");
        // ⭐ 权限校验：只能改自己的课
        if (!teacherId.equals(course.getTeacherId())) {
            return R.error("无权操作该课程");
        }

        // 只更新允许的字段
        course.setCourseName(dto.getCourseName());
        course.setCredits(dto.getCredits());
        course.setMaxCapacity(dto.getMaxCapacity());
        course.setStartDate(dto.getStartDate());
        course.setEndDate(dto.getEndDate());
        course.setSelectionOpen(dto.getSelectionOpen());  // 开放选课开关

        courseInfoService.updateById(course);
        return R.success("课程更新成功");
    }

    /**
     * 4. 结课/开放课程
     * PUT /api/teacher/courses/{courseId}/status
     */
    @PutMapping("/teacher/courses/{courseId}/status")
    public R<String> updateCourseStatus(@PathVariable Integer courseId,
                                        @RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        String status = body.get("status");  // "OPEN" 或 "CLOSED"

        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        CourseInfo course = courseInfoService.getById(courseId);

        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权操作");
        }

        course.setStatus(status);
        courseInfoService.updateById(course);
        return R.success("状态更新成功");
    }

    /**
     * 模糊查询课程列表（不分页）
     * GET /courses?courseName=数据库
     */
    @GetMapping("/courses")
    public R<List<CourseInfo>> listCourses(@RequestParam(required = false) String courseName) {
        List<CourseInfo> list = courseInfoService.listByCourseName(courseName);
        return R.success(list);
    }

    /**
     * 模糊查询 + 分页
     * GET /courses/page?courseName=数据库&page=1&size=10
     */
    @GetMapping("/course/page")
    public R<Page<CourseInfo>> pageCourses(
            @RequestParam(required = false) String courseName,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {

        Page<CourseInfo> result = courseInfoService.pageByCourseName(courseName, page, size);
        return R.success(result);
    }

    /**
     * 多条件组合查询
     * GET /courses/search?courseName=数据库&status=OPEN&teacherId=2
     */
    @GetMapping("/course/search")
    public R<List<CourseInfo>> searchCourses(
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer teacherId) {

        List<CourseInfo> list = courseInfoService.listByCondition(courseName, status, teacherId);
        return R.success(list);
    }

}
