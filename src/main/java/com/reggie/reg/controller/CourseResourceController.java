package com.reggie.reg.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.AuditLog;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.CourseResource;
import com.reggie.reg.service.IAuditLogService;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.ICourseResourceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class CourseResourceController {

    private final ICourseResourceService resourceService;
    private final ICourseInfoService courseService;
    private final IAuditLogService auditLogService;

    /**
     * 1. 获取教师资源列表（按课程过滤）
     * GET /api/teacher/resources?courseId=1&title=&type=&auditStatus=&page=1&size=10
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

        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
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
     * 2. 上传资源
     * POST /api/teacher/resources
     */
    @PostMapping("/teacher/resources")
    public R<String> uploadResource(@RequestBody CourseResource resource, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        // 权限校验
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
     * 3. 更新资源
     * PUT /api/teacher/resources/{resourceId}
     */
    @PutMapping("/teacher/resources/{resourceId}")
    public R<String> updateResource(@PathVariable Integer resourceId,
                                    @RequestBody CourseResource dto,
                                    HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        CourseResource resource = resourceService.getById(resourceId);
        if (resource == null) return R.error("资源不存在");

        // 权限校验
        CourseInfo course = courseService.getById(resource.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权操作该资源");
        }

        // 只更新允许的字段
        resource.setTitle(dto.getTitle());
        resource.setType(dto.getType());
        resource.setFileUrl(dto.getFileUrl());
        // 只有当 oriName 非空时才更新（避免编辑时不传导致被覆盖为空）
        if (dto.getOriName() != null && !dto.getOriName().trim().isEmpty()) {
            resource.setOriName(dto.getOriName());
        }
        // 注意：不更新 auditStatus，审核状态由管理员控制

        resourceService.updateById(resource);
        return R.success("资源更新成功");
    }

    /**
     * 4. 删除资源
     * DELETE /api/teacher/resources/{resourceId}
     */
    @DeleteMapping("/teacher/resources/{resourceId}")
    public R<String> deleteResource(@PathVariable Integer resourceId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        CourseResource resource = resourceService.getById(resourceId);
        if (resource == null) return R.error("资源不存在");

        // 权限校验
        CourseInfo course = courseService.getById(resource.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权删除该资源");
        }

        // 已审核通过的资源不允许删除
        if ("PASS".equals(resource.getAuditStatus())) {
            return R.error("已通过审核的资源不能删除，请联系管理员");
        }

        resourceService.removeById(resourceId);
        return R.success("删除成功");
    }

    /**
     * 5. 重新提交审核（已拒绝的资源）
     * PUT /api/teacher/resources/{resourceId}/resubmit
     */
    @PutMapping("/teacher/resources/{resourceId}/resubmit")
    public R<String> resubmitAudit(@PathVariable Integer resourceId, HttpServletRequest request) {
        CourseResource resource = resourceService.getById(resourceId);
        if (resource == null) return R.error("资源不存在");

        // 只有被拒绝的资源才能重新提交
        if (!"REJECT".equals(resource.getAuditStatus())) {
            return R.error("只有被拒绝的资源才能重新提交审核");
        }

        // 重置审核状态
        resource.setAuditStatus("PENDING");
        resourceService.updateById(resource);

        // 更新审核记录
        updateAuditLogForResource(resourceId);

        return R.success("已重新提交审核");
    }

    // ========== 内部辅助方法 ==========

    private void createAuditLogForResource(Integer resourceId, Integer uploaderId) {
        AuditLog audit = new AuditLog();
        audit.setTargetType("RESOURCE");
        audit.setTargetId(resourceId);
        audit.setResult("PENDING");
        audit.setAuditTime(LocalDateTime.now());
        auditLogService.save(audit);
    }

    private void updateAuditLogForResource(Integer resourceId) {
        AuditLog latestAudit = auditLogService.getOne(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getTargetType, "RESOURCE")
                        .eq(AuditLog::getTargetId, resourceId)
                        .orderByDesc(AuditLog::getAuditTime)
                        .last("LIMIT 1")
        );

        if (latestAudit != null) {
            latestAudit.setResult("PENDING");
            latestAudit.setAuditorId(null);
            latestAudit.setReason(null);
            latestAudit.setAuditTime(LocalDateTime.now());
            auditLogService.updateById(latestAudit);
        } else {
            createAuditLogForResource(resourceId, null);
        }
    }
}