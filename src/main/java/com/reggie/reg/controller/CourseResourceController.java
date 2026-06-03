package com.reggie.reg.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.AuditLog;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.CourseResource;
import com.reggie.reg.entity.CourseSelection;
import com.reggie.reg.service.IAuditLogService;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.ICourseResourceService;
import com.reggie.reg.service.ICourseSelectionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CourseResourceController {

    private final ICourseResourceService resourceService;
    private final ICourseInfoService courseService;
    private final IAuditLogService auditLogService;
    private final ICourseSelectionService selectionService;


    /**
     * 获取教师课程资源列表接口
     *
     * @param courseId    课程ID（必填）
     * @param title       资源标题（可选）
     * @param type        资源类型（可选）
     * @param auditStatus 审核状态（可选）
     * @param page        当前页码（默认为1）
     * @param size        每页条数（默认为10）
     * @param request     HTTP请求对象，用于获取session中的用户信息
     * @return 返回资源分页数据
     */
    @GetMapping("/teacher/resources")
    public R<Page<CourseResource>> listResources(
            @RequestParam Integer courseId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        // 从session中获取教师ID，用于权限校验
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        // 检查教师是否登录，未登录则返回错误信息
        if (teacherId == null) return R.error("未登录");

        // 权限校验：课程属于当前教师
        CourseInfo course = courseService.getById(courseId);
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权访问该课程");
        }

        // 构建查询条件
        LambdaQueryWrapper<CourseResource> query = new LambdaQueryWrapper<>();
        query.eq(CourseResource::getCourseId, courseId);
        query.like(StringUtils.isNotBlank(title), CourseResource::getTitle, title);
        query.eq(StringUtils.isNotBlank(type), CourseResource::getType, type);
        query.eq(StringUtils.isNotBlank(auditStatus), CourseResource::getAuditStatus, auditStatus);
        query.orderByDesc(CourseResource::getCreateTime);

        return R.success(resourceService.page(new Page<>(page, size), query));
    }


    /**
     * 处理教师上传课程资源的请求
     *
     * @param resource 包含资源信息的课程资源对象
     * @param request  HTTP请求对象，用于获取会话信息
     * @return 返回操作结果，成功或失败信息
     */
    @PostMapping("/teacher/resources")
    public R<String> uploadResource(@RequestBody CourseResource resource, HttpServletRequest request) {
        // 从会话中获取当前登录的教师ID
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        // 权限校验：检查课程是否存在且当前教师是否有权限上传资源
        CourseInfo course = courseService.getById(resource.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权在该课程下上传资源");
        }

        // 设置默认值
        resource.setUploaderId(teacherId);
        resource.setAuditStatus("PENDING");  // 新资源默认待审核
        resource.setCreateTime(LocalDateTime.now());

        // 保存资源
        resourceService.save(resource);

        // 自动创建审核记录
        createAuditLogForResource(resource.getResourceId(), teacherId);

        return R.success("资源上传成功，等待审核");
    }


    /**
     * 更新课程资源的接口方法
     *
     * @param resourceId 资源ID，路径变量
     * @param dto        包含更新后资源信息的DTO对象
     * @param request    HTTP请求对象，用于获取session中的用户信息
     * @return 返回操作结果，成功或失败信息
     */
    @PutMapping("/teacher/resources/{resourceId}")
    public R<String> updateResource(@PathVariable Integer resourceId,
                                    @RequestBody CourseResource dto,
                                    HttpServletRequest request) {
        // 从session中获取教师ID
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        // 根据资源ID查询资源信息
        CourseResource resource = resourceService.getById(resourceId);
        if (resource == null) return R.error("资源不存在");

        // 权限校验：检查资源是否存在且当前教师是否有权限操作
        CourseInfo course = courseService.getById(resource.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权操作该资源");
        }

        // 只更新允许的字段，不更新审核状态等敏感字段
        resource.setTitle(dto.getTitle());
        resource.setType(dto.getType());
        resource.setFileUrl(dto.getFileUrl());
        // 只有当 oriName 非空时才更新（避免编辑时不传导致被覆盖为空）
        if (dto.getOriName() != null && !dto.getOriName().trim().isEmpty()) {
            resource.setOriName(dto.getOriName());
        }

        // 注意：不更新 auditStatus，审核状态由管理员控制

        // 执行更新操作
        resourceService.updateById(resource);
        return R.success("资源更新成功");
    }


    /**
     * 删除教师资源的接口方法
     *
     * @param resourceId 要删除的资源ID，通过路径变量传递
     * @param request    HTTP请求对象，用于获取会话中的教师ID
     * @return 返回操作结果，包含成功或失败信息
     */
    @DeleteMapping("/teacher/resources/{resourceId}")
    public R<String> deleteResource(@PathVariable Integer resourceId, HttpServletRequest request) {
        // 从会话中获取当前登录教师的ID
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        // 根据资源ID查询资源信息
        CourseResource resource = resourceService.getById(resourceId);
        // 如果资源不存在，返回错误信息
        if (resource == null) return R.error("资源不存在");

        // 权限校验：检查教师是否有权限删除该资源
        // 首先获取资源所属的课程信息
        CourseInfo course = courseService.getById(resource.getCourseId());
        // 如果课程不存在或教师不是该课程的任课教师，则返回无权删除的错误信息
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权删除该资源");
        }

        // 已审核通过的资源不允许删除 其实没有这样的约束也可以
        if ("PASS".equals(resource.getAuditStatus())) {
            return R.error("已通过审核的资源不能删除");
        }

        resourceService.removeById(resourceId);
        return R.success("删除成功");
    }


    /**
     * 处理教师重新提交资源审核的请求
     *
     * @param resourceId 资源ID，用于标识需要重新提交的资源
     * @param request    HTTP请求对象，用于获取请求相关信息
     * @return 返回操作结果，成功或失败信息
     */
    @PutMapping("/teacher/resources/{resourceId}/resubmit")
    public R<String> resubmitAudit(@PathVariable Integer resourceId, HttpServletRequest request) {
        // 根据资源ID获取资源信息
        CourseResource resource = resourceService.getById(resourceId);
        // 如果资源不存在，返回错误信息
        if (resource == null) return R.error("资源不存在");

        // 只有被拒绝的资源才能重新提交
        // 检查资源的审核状态是否为"REJECT"
        if (!"REJECT".equals(resource.getAuditStatus())) {
            return R.error("只有被拒绝的资源才能重新提交审核");
        }

        // 重置审核状态为"待审核"(PENDING)
        resource.setAuditStatus("PENDING");
        // 更新资源信息到数据库
        resourceService.updateById(resource);

        // 更新审核记录
        updateAuditLogForResource(resourceId);

        return R.success("已重新提交审核");
    }

    // ========== 内部辅助方法 ==========

    /**
     * 为资源创建审核日志
     *
     * @param resourceId 资源ID
     * @param uploaderId 上传者ID
     */
    private void createAuditLogForResource(Integer resourceId, Integer uploaderId) {
        // 创建一个新的审核日志对象
        AuditLog audit = new AuditLog();
        // 设置目标类型为"RESOURCE"
        audit.setTargetType("RESOURCE");
        // 设置目标ID为传入的资源ID
        audit.setTargetId(resourceId);
        // 设置审核结果为"PENDING"
        audit.setResult("PENDING");
        // 设置审核时间为当前时间
        audit.setAuditTime(LocalDateTime.now());
        // 保存审核日志到数据库
        auditLogService.save(audit);
    }

    /**
     * 更新指定资源的审计日志
     * 该方法会查找最新的资源审计记录，并将其状态重置为"待处理"
     * 如果没有找到审计记录，则会创建一条新的审计记录
     *
     * @param resourceId 要更新的资源ID
     */
    private void updateAuditLogForResource(Integer resourceId) {
        // 查询指定资源的最新审计记录
        AuditLog latestAudit = auditLogService.getOne(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getTargetType, "RESOURCE")  // 设置目标类型为"RESOURCE"
                        .eq(AuditLog::getTargetId, resourceId)    // 设置目标ID为传入的resourceId
                        .orderByDesc(AuditLog::getAuditTime)     // 按审计时间降序排序
                        .last("LIMIT 1")                          // 只取最新的一条记录
        );

        // 如果存在审计记录，则更新该记录
        if (latestAudit != null) {
            // 重置审计结果为"待处理"
            latestAudit.setResult("PENDING");
            // 清空审计人员ID
            latestAudit.setAuditorId(null);
            // 清空审计原因
            latestAudit.setReason(null);
            // 更新审计时间为当前时间
            latestAudit.setAuditTime(LocalDateTime.now());
            // 执行更新操作
            auditLogService.updateById(latestAudit);
        } else {
            // 如果不存在审计记录，则创建一条新的审计记录
            createAuditLogForResource(resourceId, null);
        }
    }


    /**
     * 获取学生已选课程的资源列表
     * GET /student/courses/{courseId}/resources?type=&title=
     */
    @GetMapping("/student/courses/{courseId}/resources")
    public R<List<Map<String, Object>>> getCourseResources(
            @PathVariable Integer courseId,
            @RequestParam(required = false) String type,      // 资源类型筛选：PPT/VIDEO/LINK/FILE
            @RequestParam(required = false) String title,     // 资源标题模糊搜索
            @RequestParam Integer studentId,
            HttpServletRequest request) {

        try {
            // 1. 权限校验：学生只能查自己选的课程
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            // 校验课程是否已选
            CourseSelection selection = selectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)
                            .eq(CourseSelection::getStudentId, studentId)
                            .eq(CourseSelection::getStatus, "SELECTED")
            );
            if (selection == null) {
                return R.error("当前课程未被选中");
            }

            // 2. 构建查询条件
            LambdaQueryWrapper<CourseResource> query = new LambdaQueryWrapper<>();
            query.eq(CourseResource::getCourseId, courseId);
            query.eq(CourseResource::getAuditStatus, "PASS");  // 只展示审核通过的资源

            if (StringUtils.isNotBlank(type)) {
                query.eq(CourseResource::getType, type);
            }
            if (StringUtils.isNotBlank(title)) {
                query.like(CourseResource::getTitle, title);
            }
            query.orderByDesc(CourseResource::getCreateTime);

            // 3. 查询资源列表
            List<CourseResource> resources = resourceService.list(query);
            if (resources.isEmpty()) {
                return R.success(new ArrayList<>());
            }

            // 4. 预加载教师姓名（资源上传者）
            List<Integer> uploaderIds = resources.stream()
                    .map(CourseResource::getUploaderId)
                    .distinct().collect(Collectors.toList());

            Map<Integer, String> teacherMap = new HashMap<>();
            if (!uploaderIds.isEmpty()) {
                // 假设通过 userService 查询教师信息，此处简化处理
                // 实际项目中需注入 ISysUserService
                // teacherMap = userService.listByIds(uploaderIds).stream()
                //     .collect(Collectors.toMap(SysUser::getUserId, SysUser::getUsername));
            }

            // 5. 组装结果（保持前端格式兼容）
            List<Map<String, Object>> resultList = resources.stream().map(r -> {
                Map<String, Object> item = new HashMap<>();
                item.put("resourceId", r.getResourceId());
                item.put("title", r.getTitle());
                item.put("type", r.getType());  // PPT/VIDEO/LINK/FILE
                item.put("fileUrl", r.getFileUrl());
                item.put("oriName", r.getOriName());  // 原始文件名，前端展示用
                item.put("createTime", r.getCreateTime());
                // 简化：直接显示"教师"，实际可关联查询教师姓名
                item.put("uploaderName", teacherMap.getOrDefault(r.getUploaderId(), "教师"));
                return item;
            }).collect(Collectors.toList());

            return R.success(resultList);

        } catch (Exception e) {
            log.error("Get course resources error", e);
            return R.success(new ArrayList<>());  // 容错返回空列表
        }
    }
}