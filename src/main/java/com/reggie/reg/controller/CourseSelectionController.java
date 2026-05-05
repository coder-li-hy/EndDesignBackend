package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.CourseSelection;
import com.reggie.reg.entity.Notification;
import com.reggie.reg.entity.SysUser;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.ICourseSelectionService;
import com.reggie.reg.service.INotificationService;
import com.reggie.reg.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 选课记录表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
public class CourseSelectionController {
    private final ICourseSelectionService selectionService;
    private final ICourseInfoService courseService;
    private final ISysUserService userService;
    private final INotificationService notificationService;


    /**
     * 获取学生已选课程或排队课程列表
     * @param studentId 学生ID
     * @param status 课程状态（SELECTED或QUEUED），默认为SELECTED
     * @param request HTTP请求对象，用于获取当前登录用户信息
     * @return 返回课程信息列表，包含课程基本信息和选课状态
     */
    @GetMapping("/student/courses/my")
    public R<List<Map<String, Object>>> getMyCourses(
            @RequestParam Integer studentId,        // 学生ID，用于查询指定学生的选课记录
            @RequestParam(required = false, defaultValue = "SELECTED") String status,  // 课程状态，可选值为SELECTED（已选）或QUEUED（排队），默认为SELECTED
            HttpServletRequest request) {         // HTTP请求对象，用于从Session中获取当前登录用户信息

        try {
            // 1. 权限校验：只能查自己的
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            // 2. 查询选课记录
            LambdaQueryWrapper<CourseSelection> query = new LambdaQueryWrapper<>();
            query.eq(CourseSelection::getStudentId, studentId);
            query.eq(CourseSelection::getStatus, status);  // SELECTED 或 QUEUED
            query.orderByDesc(CourseSelection::getSelectTime);

            List<CourseSelection> selections = selectionService.list(query);
            if (selections.isEmpty()) {
                return R.success(new ArrayList<>());
            }

            // 3. 预加载关联数据
            List<Integer> courseIds = selections.stream()
                    .map(CourseSelection::getCourseId)
                    .distinct().collect(Collectors.toList());
            Map<Integer, CourseInfo> courseMap = courseService.listByIds(courseIds)
                    .stream().collect(Collectors.toMap(CourseInfo::getCourseId, c -> c));

            List<Integer> teacherIds = courseMap.values().stream()
                    .map(CourseInfo::getTeacherId)
                    .distinct().collect(Collectors.toList());
            Map<Integer, String> teacherNameMap = userService.listByIds(teacherIds)
                    .stream().collect(Collectors.toMap(SysUser::getUserId, SysUser::getUsername));

            // 4. 组装结果
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (CourseSelection sel : selections) {
                CourseInfo course = courseMap.get(sel.getCourseId());
                if (course == null) continue;

                Map<String, Object> item = new HashMap<>();
                item.put("courseId", course.getCourseId());
                item.put("courseName", course.getCourseName());
                item.put("credits", course.getCredits());
                item.put("startDate", course.getStartDate());
                item.put("endDate", course.getEndDate());
                item.put("maxCapacity", course.getMaxCapacity());
                item.put("currentCount", course.getCurrentCount());
                item.put("status", course.getStatus());
                item.put("teacherName", teacherNameMap.get(course.getTeacherId()));
                item.put("selectionStatus", sel.getStatus());  // SELECTED/QUEUED
                item.put("selectTime", sel.getSelectTime());
                // 排队位置（简化：按选课时间排序，实际可计算）
                if ("QUEUED".equals(status)) {
                    item.put("queuePosition", selections.indexOf(sel) + 1);
                }

                resultList.add(item);
            }

            return R.success(resultList);

        } catch (Exception e) {
            System.err.println("Get my courses error: " + e.getMessage());
            return R.success(new ArrayList<>());  // 容错：返回空列表
        }
    }


    /**
     * 处理学生取消选课请求的接口方法
     * @param courseId 课程ID，路径变量
     * @param params 包含学生ID的请求体参数
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回操作结果，成功或失败信息
     */
    @PostMapping("/student/courses/{courseId}/cancel")
    public R<String> cancelSelection(
            @PathVariable Integer courseId,           // 从路径中获取的课程ID
            @RequestBody Map<String, Integer> params, // 请求体中的参数，包含学生ID
            HttpServletRequest request) {           // HTTP请求对象，用于获取会话信息

        try {
            // 从请求参数中获取学生ID
            Integer studentId = params.get("studentId");
            if (studentId == null) {
                return R.error("参数错误");
            }

            // 1. 权限校验：检查当前登录用户是否为操作本人
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权操作");
            }

            // 2. 查询选课记录：根据课程ID和学生ID查询对应的选课记录
            CourseSelection selection = selectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)  // 匹配课程ID
                            .eq(CourseSelection::getStudentId, studentId) // 匹配学生ID
            );

            if (selection == null) {
                return R.error("未找到选课记录");
            }

            // 3. 删除记录（简化：直接删除，实际可标记为取消）
            selectionService.removeById(selection.getSelectionId());

            // 4. （可选）更新课程当前人数
            if ("SELECTED".equals(selection.getStatus())) {
                CourseInfo course = courseService.getById(courseId);
                if (course != null && course.getCurrentCount() != null && course.getCurrentCount() > 0) {
                    course.setCurrentCount(course.getCurrentCount() - 1);
                    courseService.updateById(course);
                }
            }

            // TODO: 有一名学生完成取消选课后 若当前学生有排队课程 则从当前课程正在排队的学生中选择select_time最早的 将其加入已选
            handleQueueBackfill(courseId);
            return R.success("取消成功");

        } catch (Exception e) {
            System.err.println("Cancel selection error: " + e.getMessage());
            return R.error("取消失败");
        }
    }


    /**
     * 获取学生可选课程市场列表
     * @param studentId 学生ID
     * @param courseName 课程名称（可选参数）
     * @param page 页码（默认为1）
     * @param size 每页大小（默认为10）
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回课程市场列表数据，包含分页信息和课程详情
     */
    @GetMapping("/student/courses/market")
    public R<Map<String, Object>> getCourseMarket(
            @RequestParam Integer studentId,        // 学生ID
            @RequestParam(required = false) String courseName,  // 课程名称，可选参数
            @RequestParam(defaultValue = "1") Integer page,     // 页码，默认为1
            @RequestParam(defaultValue = "10") Integer size,    // 每页大小，默认为10
            HttpServletRequest request) {          // HTTP请求对象

        try {
            // 1. 权限校验：验证当前用户是否为该学生本人
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            // 2. 查询可选课程条件：构建查询条件，筛选开放选课且状态为开放中的课程
            LambdaQueryWrapper<CourseInfo> query = new LambdaQueryWrapper<>();
            query.eq(CourseInfo::getSelectionOpen, true);  // 开放选课
            query.eq(CourseInfo::getStatus, "OPEN");        // 课程开放中
            query.like(StringUtils.isNotBlank(courseName), CourseInfo::getCourseName, courseName);  // 根据课程名模糊查询
            query.orderByDesc(CourseInfo::getCourseId);     // 按课程ID降序排列

            // 3. 分页查询：执行分页查询获取课程列表
            Page<CourseInfo> coursePage = courseService.page(new Page<>(page, size), query);

            // 如果查询结果为空，返回空结果集
            if (coursePage.getRecords().isEmpty()) {
                return R.success(buildEmptyResult());
            }

            // 4. 预加载关联数据：优化查询性能，预加载关联数据
            List<Integer> courseIds = coursePage.getRecords().stream()
                    .map(CourseInfo::getCourseId).collect(Collectors.toList());

            // 4.1 查教师姓名
            List<Integer> teacherIds = coursePage.getRecords().stream()
                    .map(CourseInfo::getTeacherId).distinct().collect(Collectors.toList());
            Map<Integer, String> teacherNameMap = userService.listByIds(teacherIds)
                    .stream().collect(Collectors.toMap(SysUser::getUserId, SysUser::getUsername));

            // 4.2 查学生是否已选/排队
            Map<Integer, String> userStatusMap = new HashMap<>();
            if (!courseIds.isEmpty()) {
                List<CourseSelection> selections = selectionService.list(
                        new LambdaQueryWrapper<CourseSelection>()
                                .eq(CourseSelection::getStudentId, studentId)
                                .in(CourseSelection::getCourseId, courseIds)
                );
                for (CourseSelection sel : selections) {
                    userStatusMap.put(sel.getCourseId(), sel.getStatus());  // SELECTED/QUEUED
                }
            }

            // 5. 组装结果
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (CourseInfo course : coursePage.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("courseId", course.getCourseId());
                item.put("courseName", course.getCourseName());
                item.put("credits", course.getCredits());
                item.put("startDate", course.getStartDate());
                item.put("endDate", course.getEndDate());
                item.put("maxCapacity", course.getMaxCapacity());
                item.put("currentCount", course.getCurrentCount());
                item.put("status", course.getStatus());
                item.put("teacherName", teacherNameMap.get(course.getTeacherId()));

                // 是否已满
                boolean isFull = course.getCurrentCount() != null &&
                        course.getMaxCapacity() != null &&
                        course.getCurrentCount() >= course.getMaxCapacity();
                item.put("isFull", isFull);

                // 用户状态（已选/排队/无）
                item.put("userStatus", userStatusMap.get(course.getCourseId()));

                resultList.add(item);
            }

            // 6. 返回
            Map<String, Object> result = new HashMap<>();
            result.put("list", resultList);
            result.put("total", coursePage.getTotal());
            return R.success(result);

        } catch (Exception e) {
            System.err.println("Get course market error: " + e.getMessage());
            return R.success(buildEmptyResult());  // 容错
        }
    }


    /**
     * 处理学生选课请求的接口方法
     * @param courseId 课程ID，从URL路径中获取
     * @param params 包含学生ID的请求参数
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回操作结果，包含成功或失败信息
     */
    @PostMapping("/student/courses/{courseId}/select")
    public R<String> selectCourse(
            @PathVariable Integer courseId,  // 从URL路径中获取的课程ID
            @RequestBody Map<String, Integer> params,  // 请求体中的参数，包含学生ID
            HttpServletRequest request) {  // HTTP请求对象

        try {
            // 从请求参数中获取学生ID
            Integer studentId = params.get("studentId");
            if (studentId == null) {
                return R.error("参数错误");
            }

            // 1. 权限校验：验证当前用户是否有权操作该学生的选课
            // 从会话中获取当前登录用户ID
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            // 检查用户是否已登录且与学生ID匹配
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权操作");
            }

            // 2. 查课程信息
            CourseInfo course = courseService.getById(courseId);
            if (course == null) {
                return R.error("课程不存在");
            }
            if (!Boolean.TRUE.equals(course.getSelectionOpen()) || !"OPEN".equals(course.getStatus())) {
                return R.error("该课程暂不可选");
            }

            // 3. 查是否已选/排队
            CourseSelection existing = selectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)
                            .eq(CourseSelection::getStudentId, studentId)
            );
            if (existing != null) {
                return R.error("您已" + ("SELECTED".equals(existing.getStatus()) ? "选择" : "排队") + "该课程");
            }

            // 4. 判断是否已满
            int currentCount = course.getCurrentCount() != null ? course.getCurrentCount() : 0;
            int maxCapacity = course.getMaxCapacity() != null ? course.getMaxCapacity() : 0;
            String status = (currentCount < maxCapacity) ? "SELECTED" : "QUEUED";

            // 5. 创建选课记录
            CourseSelection selection = new CourseSelection();
            selection.setCourseId(courseId);
            selection.setStudentId(studentId);
            selection.setStatus(status);
            selection.setSelectTime(LocalDateTime.now());
            selectionService.save(selection);

            // 6. 如果直接选中，更新课程当前人数
            if ("SELECTED".equals(status)) {
                course.setCurrentCount(currentCount + 1);
                courseService.updateById(course);
            }

            String msg = "SELECTED".equals(status) ? "选课成功" : "已加入排队";
            return R.success(msg);

        } catch (Exception e) {
            System.err.println("Select course error: " + e.getMessage());
            return R.error("操作失败");
        }
    }

    // 辅助方法：构建空结果
    /**
     * 构建一个空的查询结果对象
     * 该方法返回一个包含空列表和总数为0的Map对象
     * 通常用于初始化查询结果，当查询无数据时返回
     *
     * @return 包含两个字段的Map对象：
     *         - "list": 一个空ArrayList，用于存放查询结果数据
     *         - "total": 整数0，表示查询结果的总数
     */
    private Map<String, Object> buildEmptyResult() {
        // 创建一个新的HashMap对象用于存放结果
        Map<String, Object> result = new HashMap<>();
        // 向结果Map中添加空列表，用于存放数据
        result.put("list", new ArrayList<>());
        // 向结果Map中添加总数，初始值为0
        result.put("total", 0);
        // 返回构建好的结果Map
        return result;
    }



    /**
     * 处理课程队列的递补功能
     * 当有学生取消选课时，系统会自动从排队队列中选取最早排队的学生进行递补
     * @param courseId 课程ID，用于查询对应课程的排队情况
     */
    private void handleQueueBackfill(Integer courseId) {
        // 1. 查询该课程当前排队的学生（按选课时间升序，取最早的）
        // 使用LambdaQueryWrapper构建查询条件，查找状态为"QUEUED"且指定课程ID的学生记录
        // 按选课时间升序排序并限制只返回1条记录，即最早排队的学生
        CourseSelection nextStudent = selectionService.getOne(
                new LambdaQueryWrapper<CourseSelection>()
                        .eq(CourseSelection::getCourseId, courseId)  // 设置课程ID条件
                        .eq(CourseSelection::getStatus, "QUEUED")    // 设置状态为排队中
                        .orderByAsc(CourseSelection::getSelectTime)  // 按选课时间升序排序
                        .last("LIMIT 1")  // 只取最早的一个
        );

        // 2. 如果没有排队学生，直接返回
        // 检查查询结果是否为空，若为空则表示没有学生排队，无需进行递补操作
        if (nextStudent == null) {
            return;
        }

        // 3. 更新该学生状态为"已选"
        // 将排队学生的状态从"QUEUED"更新为"SELECTED"，表示选课成功
        nextStudent.setStatus("SELECTED");
        selectionService.updateById(nextStudent);

        // 4. 更新课程当前人数 +1（递补成功）
        // 获取课程信息，并将当前选课人数加1，反映递补成功后的实际选课情况
        CourseInfo course = courseService.getById(courseId);
        if (course != null && course.getCurrentCount() != null) {
            course.setCurrentCount(course.getCurrentCount() + 1);
            courseService.updateById(course);
        }

        // 5. （可选）发送通知给递补成功的学生
        // 向成功递补选课的学生发送通知，告知其选课成功
         notifyStudent(nextStudent.getStudentId(), courseId, "恭喜！您已成功选上课程");
    }

    // 在 handleQueueBackfill 末尾添加
    /**
     * 通知学生选课递补信息
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param message 通知内容
     */
    private void notifyStudent(Integer studentId, Integer courseId, String message) {
        // 简化：记录到通知表
        Notification notify = new Notification();
        notify.setReceiverId(studentId);
        notify.setPublisherId(0);  // 系统通知
        notify.setCourseId(courseId);
        notify.setType("SYSTEM");
        notify.setTitle("选课递补通知");
        notify.setContent(message);
        notify.setPublishTime(LocalDateTime.now());
        notificationService.save(notify);
    }
}
