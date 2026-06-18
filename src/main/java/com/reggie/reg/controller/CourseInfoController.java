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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CourseInfoController {

    private final ICourseInfoService courseInfoService;




    /**
     * 获取教师课程列表接口 分页查询
     * @param courseName 课程名称（可选参数）
     * @param status 课程状态（可选参数）
     * @param page 页码（默认值1）
     * @param size 每页条数（默认值10）
     * @param request HTTP请求对象
     * @return 返回分页课程信息
     */
    @GetMapping("/teacher/courses")
    public R<Page<CourseInfo>> listCourses(
            @RequestParam(required = false) String courseName,  // 可选的课程名称参数
            @RequestParam(required = false) String status,      // 可选的课程状态参数
            @RequestParam(defaultValue = "1") Integer page,     // 页码参数，默认为1
            @RequestParam(defaultValue = "10") Integer size,    // 每页条数参数，默认为10
            HttpServletRequest request) {  // HTTP请求对象，用于获取session信息

        // 1. 获取当前教师 ID
        // 从session中获取当前登录教师的ID，如果未登录则返回错误信息
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        if (teacherId == null) {
            return R.error("未登录");
        }

        // 2. 构建查询条件
        // 使用LambdaQueryWrapper构建动态查询条件
        LambdaQueryWrapper<CourseInfo> query = new LambdaQueryWrapper<>();
        query.eq(CourseInfo::getTeacherId, teacherId);  // 只查自己的课程 - 确保教师只能查看自己的课程
        query.like(StringUtils.isNotBlank(courseName), CourseInfo::getCourseName, courseName);  // 根据课程名称模糊查询
        query.eq(StringUtils.isNotBlank(status), CourseInfo::getStatus, status);  // 根据课程状态精确查询
        query.orderByDesc(CourseInfo::getCourseId);  // 按课程ID降序排序

        // 返回分页查询结果
        return R.success(courseInfoService.page(new Page<>(page, size), query));
    }



    /**
     * 创建课程的接口
     * @param request HTTP请求对象，用于获取Session信息
     * @param dto 课程数据传输对象，包含课程相关信息
     * @return 返回操作结果，成功时包含课程信息，失败时包含错误信息
     */
    @PostMapping("/teacher/courses")
    public R<CourseInfo> createCourse(HttpServletRequest request,
                                      @RequestBody CourseDTO dto) {
        // 获取Session对象，如果不存在则返回未登录错误
        HttpSession session = request.getSession(false);
        if (session == null) {
            return R.error("NOT_LOGIN");
        }
        // 检测传入时间是否合法
        // 检查开始时间和结束时间是否为空，以及开始时间是否晚于结束时间
        if (dto.getStartDate() == null || dto.getEndDate() == null||(dto.getStartDate().isAfter(dto.getEndDate()))) {
            return R.error("课程时间不合法");
        }

        // 从 Session 获取用户信息（安全）
        Integer userId = (Integer) session.getAttribute("sys_user");
        String role = (String) session.getAttribute("sys_user_role");

        // 校验角色（关键！）
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
     * 更新课程信息的接口
     * @param courseId 课程ID，路径变量
     * @param dto 包含课程更新信息的DTO对象
     * @param request HTTP请求对象，用于获取session中的教师ID
     * @return 返回操作结果，R对象包含操作状态和消息
     */
    @PutMapping("/teacher/courses/{courseId}")
    public R<String> updateCourse(@PathVariable Integer courseId,
                                  @RequestBody CourseInfo dto,
                                  HttpServletRequest request) {
        // 从session中获取当前登录教师的ID
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        // 根据课程ID查询课程信息
        CourseInfo course = courseInfoService.getById(courseId);
        // 如果课程不存在，返回错误信息
        if (course == null) return R.error("课程不存在");
        // 权限校验：只能改自己的课
        // 检查当前教师是否有权限修改该课程（教师ID必须与课程创建者ID一致）
        if (!teacherId.equals(course.getTeacherId())) {
            return R.error("无权操作该课程");
        }

        // 只更新允许的字段，从DTO中获取更新后的值
        course.setCourseName(dto.getCourseName());     // 课程名称
        course.setCredits(dto.getCredits());           // 课程学分
        course.setMaxCapacity(dto.getMaxCapacity());    // 课程最大容量
        course.setStartDate(dto.getStartDate());       // 课程开始日期
        course.setEndDate(dto.getEndDate());           // 课程结束日期
        course.setSelectionOpen(dto.getSelectionOpen());  // 开放选课开关

        // 更新课程信息到数据库
        courseInfoService.updateById(course);
        // 返回成功信息
        return R.success("课程更新成功");
    }


    /**
     * 更新课程状态接口
     * @param courseId 课程ID路径变量
     * @param body 包含状态信息的请求体
     * @param request HTTP请求对象，用于获取session中的教师ID
     * @return 返回操作结果，成功或失败信息
     */
    @PutMapping("/teacher/courses/{courseId}/status")
    public R<String> updateCourseStatus(@PathVariable Integer courseId,
                                        @RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        //
        String status = body.get("status");  // "OPEN" 或 "CLOSED"

        // 获取当前用户id
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        CourseInfo course = courseInfoService.getById(courseId);

        // 权限校验：只能改自己的课
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权操作");
        }

        // 更新课程状态
        course.setStatus(status);
        // 调用Mybatis—plus进行更新
        courseInfoService.updateById(course);
        return R.success("状态更新成功");
    }

    /**
     * 更新课程状态接口
     * @param courseId 课程ID路径变量
     * @param body 包含状态信息的请求体
     * @param request HTTP请求对象，用于获取session中的教师ID
     * @return 返回操作结果，成功或失败信息
     */
    @PutMapping("/teacher/courses/{courseId}/selection")
    public R<String> updateCourseSelect(@PathVariable Integer courseId,
                                        @RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        //
        String status = body.get("selectionOpen");  // "OPEN" 或 "CLOSED"
        Boolean selectionOpen = Boolean.parseBoolean(status);
        log.info("status: {}", status);

        // 获取当前用户id
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        CourseInfo course = courseInfoService.getById(courseId);

        // 权限校验：只能改自己的课
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权操作");
        }
        course.setSelectionOpen(selectionOpen);

        // 调用Mybatis—plus进行更新
        courseInfoService.updateById(course);
        return R.success("状态更新成功");
    }

    /**
     * 获取课程列表接口
     * @param courseName 课程名称，非必填参数
     * @return 返回课程列表数据，封装在R对象中
     */
    @GetMapping("/courses") // HTTP GET请求映射到/courses路径
    public R<List<CourseInfo>> listCourses(@RequestParam(required = false) String courseName) {
        // 根据课程名称查询课程信息列表，courseName为空时查询所有课程
        List<CourseInfo> list = courseInfoService.listByCourseName(courseName);
        // 将查询结果封装到R对象中并返回，表示请求成功
        return R.success(list);
    }


    /**
     * 分页查询课程信息接口
     * @param courseName 课程名称，可选参数
     * @param page 当前页码，默认值为1
     * @param size 每页大小，默认值为10
     * @return 返回分页查询结果，包含课程信息列表
     */
    @GetMapping("/course/page") // HTTP GET请求映射到/course/page路径
    public R<Page<CourseInfo>> pageCourses( // 定义返回类型为R<Page<CourseInfo>>的响应对象
            @RequestParam(required = false) String courseName, // 课程名称参数，非必需
            @RequestParam(defaultValue = "1") long page, // 页码参数，默认值为1
            @RequestParam(defaultValue = "10") long size) { // 每页大小参数，默认值为10

        Page<CourseInfo> result = courseInfoService.pageByCourseName(courseName, page, size); // 调用服务层方法获取分页结果
        return R.success(result); // 返回成功响应，包含分页数据
    }


/**
 * 根据条件搜索课程信息
 * GET请求，映射到"/course/search"路径
 * @param courseName 课程名称，可选参数
 * @param status 课程状态，可选参数
 * @param teacherId 教师ID，可选参数
 * @return 返回R对象，包含课程信息列表
 */
    @GetMapping("/course/search")
    public R<List<CourseInfo>> searchCourses(
            @RequestParam(required = false) String courseName,    // 课程名称参数，非必需
            @RequestParam(required = false) String status,         // 课程状态参数，非必需
            @RequestParam(required = false) Integer teacherId) {   // 教师ID参数，非必需

    // 调用courseInfoService的listByCondition方法，根据条件查询课程信息列表
        List<CourseInfo> list = courseInfoService.listByCondition(courseName, status, teacherId);
    // 返回成功响应，包含查询到的课程信息列表
        return R.success(list);
    }

}
