package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.dto.*;
import com.reggie.reg.entity.*;
import com.reggie.reg.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 审核日志表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
public class AuditLogController {

    private final IAuditLogService auditLogService;
    private final ICourseInfoService courseInfoService;
    private final ISysUserService sysUserService;
    private final ICourseResourceService courseResourceService;
    private final ICourseQaService courseQaService;
    private final ISubmissionService submissionService;
    private final IAssignmentService assignmentService;

    /**
     * 获取审核统计
     */
    @GetMapping("/stats")
    public R<AuditStatsVO> getAuditStats() {
        AuditStatsVO stats = auditLogService.getAuditStats();
        return R.success(stats);
    }

    /**
     * 分页查询审核列表
     */
    @GetMapping("/list")
    public R<Page<AuditLogVO>> listAudit(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        LambdaQueryWrapper<AuditLog> query = new LambdaQueryWrapper<>();
        query.eq(StringUtils.isNotBlank(targetType), AuditLog::getTargetType, targetType);
        query.eq(StringUtils.isNotBlank(status), AuditLog::getResult, status);
        query.like(StringUtils.isNotBlank(keyword), AuditLog::getReason, keyword);
        query.orderByDesc(AuditLog::getAuditTime);

        Page<AuditLog> result = auditLogService.page(new Page<>(page, size), query);

        // 关联查询资源/问答/作业信息
        List<AuditLogVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<AuditLogVO> voPage = new Page<>();
        voPage.setRecords(voList);
        voPage.setTotal(result.getTotal());
        voPage.setSize(result.getSize());
        voPage.setCurrent(result.getCurrent());

        return R.success(voPage);
    }

    /**
     * 通过审核
     */
    @PostMapping("/{auditId}/approve")
    public R<String> approveAudit(@PathVariable Integer auditId, HttpServletRequest request) {
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        auditLogService.approve(auditId, adminId);
        return R.success("审核通过");
    }

    /**
     * 拒绝审核
     */
    @PostMapping("/{auditId}/reject")
    public R<String> rejectAudit(@PathVariable Integer auditId,
                                 @RequestBody RejectDTO dto,
                                 HttpServletRequest request) {
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        auditLogService.reject(auditId, adminId, dto.getReason());
        return R.success("已拒绝");
    }

    /**
     * 批量通过
     */
    @PostMapping("/batch/approve")
    public R<String> batchApprove(@RequestBody BatchAuditDTO dto, HttpServletRequest request) {
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        auditLogService.batchApprove(dto.getAuditIds(), adminId);
        return R.success("批量通过成功");
    }

    /**
     * 批量拒绝
     */
    @PostMapping("/batch/reject")
    public R<String> batchReject(@RequestBody BatchRejectDTO dto, HttpServletRequest request) {
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        auditLogService.batchReject(dto.getAuditIds(), adminId, dto.getReason());
        return R.success("批量拒绝成功");
    }

    /**
     * 重审（重置状态为待审核）
     */
    @PostMapping("/{auditId}/reaudit")
    public R<String> reaudit(@PathVariable Integer auditId) {
        auditLogService.resetToPending(auditId);
        return R.success("已重置为待审核");
    }

    /**
     * 获取审核详情
     */
    @GetMapping("/{auditId}/detail")
    public R<AuditLogDetailVO> getAuditDetail(@PathVariable Integer auditId) {
        AuditLogDetailVO detail = auditLogService.getDetail(auditId);
        return R.success(detail);
    }

    // 内部转换方法
// ========== 修复后的 convertToVO 方法 ==========
    private AuditLogVO convertToVO(AuditLog log) {
        if (log == null) return null;

        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(log, vo);

        // 设置中文文本
        vo.setTargetTypeText(getTargetTypeText(log.getTargetType()));
        vo.setResultText(getResultText(log.getResult()));

        // 根据类型关联查询内容（转换为 SimpleVO）
        if ("RESOURCE".equals(log.getTargetType())) {
            CourseResource resource = courseResourceService.getById(log.getTargetId());
            vo.setResource(convertToResourceSimpleVO(resource));
        } else if ("QA".equals(log.getTargetType())) {
            CourseQa qa = courseQaService.getById(log.getTargetId());
            vo.setQa(convertToQaSimpleVO(qa));
        } else if ("SUBMISSION".equals(log.getTargetType())) {
            Submission submission = submissionService.getById(log.getTargetId());
            vo.setSubmission(convertToSubmissionSimpleVO(submission));
        }

        // 查询提交者信息（转换为 UserSimpleVO）
        Integer submitterId = extractSubmitterId(log);
        if (submitterId != null) {
            SysUser submitter = sysUserService.getById(submitterId);
            vo.setSubmitter(convertToUserSimpleVO(submitter));
        }

        // 查询审核人信息（转换为 UserSimpleVO）
        if (log.getAuditorId() != null) {
            SysUser auditor = sysUserService.getById(log.getAuditorId());
            vo.setAuditor(convertToUserSimpleVO(auditor));
        }

        return vo;
    }
    /**
     * CourseResource 实体 → ResourceSimpleVO
     */
    private ResourceSimpleVO convertToResourceSimpleVO(CourseResource resource) {
        if (resource == null) return null;

        ResourceSimpleVO simple = new ResourceSimpleVO();
        // 拷贝基础字段
        BeanUtils.copyProperties(resource, simple);

        // 额外关联课程名称（前端展示用）
        if (resource.getCourseId() != null) {
            CourseInfo course = courseInfoService.getById(resource.getCourseId());
            if (course != null) {
                simple.setCourseName(course.getCourseName());
            }
        }

        return simple;
    }

    /**
     * CourseQa 实体 → QaSimpleVO
     */
    private QaSimpleVO convertToQaSimpleVO(CourseQa qa) {
        if (qa == null) return null;

        QaSimpleVO simple = new QaSimpleVO();
        BeanUtils.copyProperties(qa, simple);

        // 额外关联课程名称
        if (qa.getCourseId() != null) {
            CourseInfo course = courseInfoService.getById(qa.getCourseId());
            if (course != null) {
                simple.setCourseName(course.getCourseName());
            }
        }

        return simple;
    }
    /**
     * Submission 实体 → SubmissionSimpleVO
     */
    private SubmissionSimpleVO convertToSubmissionSimpleVO(Submission submission) {
        if (submission == null) return null;

        SubmissionSimpleVO simple = new SubmissionSimpleVO();
        BeanUtils.copyProperties(submission, simple);

        // 额外关联作业标题
        if (submission.getAssignmentId() != null) {
            Assignment assignment = assignmentService.getById(submission.getAssignmentId());
            if (assignment != null) {
                simple.setAssignmentTitle(assignment.getTitle());
            }
        }

        return simple;
    }

    /**
     * SysUser 实体 → UserSimpleVO
     * ⚠️ 只返回展示字段，排除 passwordHash 等敏感信息
     */
    private UserSimpleVO convertToUserSimpleVO(SysUser user) {
        if (user == null) return null;

        UserSimpleVO simple = new UserSimpleVO();
        // 手动设置需要的字段（避免拷贝敏感字段）
        simple.setUserId(user.getUserId());
        simple.setUsername(user.getUsername());
        simple.setRole(user.getRole());
        // 设置中文角色（前端直接显示用）
        simple.setRoleText(getRoleText(user.getRole()));

        return simple;
    }

    /**
     * 用户角色枚举转中文显示
     * @param role ADMIN / TEACHER / STUDENT
     * @return 管理员 / 教师 / 学生
     */
    private String getRoleText(String role) {
        if (role == null) return "";

        return switch (role) {
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学生";
            default -> role;  // 未知角色返回原值
        };
    }
    /**
     * 内容类型枚举转中文显示
     * @param type RESOURCE / QA / SUBMISSION
     * @return 课程资源 / 问答互动 / 作业提交
     */
    private String getTargetTypeText(String type) {
        if (type == null) return "";

        return switch (type) {
            case "RESOURCE" -> "课程资源";
            case "QA" -> "问答互动";
            case "SUBMISSION" -> "作业提交";
            default -> type;  // 未知类型返回原值
        };
    }

    /**
     * 从审核记录中提取内容提交者的用户 ID
     * 根据内容类型从不同实体中获取提交者
     */
    private Integer extractSubmitterId(AuditLog log) {
        if (log == null || log.getTargetId() == null) {
            return null;
        }

        String targetType = log.getTargetType();
        Integer targetId = log.getTargetId();

        if ("RESOURCE".equals(targetType)) {
            // 资源的提交者 = 上传教师
            CourseResource resource = courseResourceService.getById(targetId);
            return resource != null ? resource.getUploaderId() : null;

        } else if ("QA".equals(targetType)) {
            // 问答的提交者 = 提问学生
            CourseQa qa = courseQaService.getById(targetId);
            return qa != null ? qa.getStudentId() : null;

        } else if ("SUBMISSION".equals(targetType)) {
            // 作业提交的提交者 = 提交作业的学生
            Submission submission = submissionService.getById(targetId);
            return submission != null ? submission.getStudentId() : null;
        }

        return null;
    }

    /**
     * 审核结果枚举转中文显示
     * @param result PENDING / PASS / REJECT
     * @return 待审核 / 已通过 / 已拒绝
     */
    private String getResultText(String result) {
        if (result == null) return "";

        return switch (result) {
            case "PENDING" -> "待审核";
            case "PASS" -> "已通过";
            case "REJECT" -> "已拒绝";
            default -> result;  // 未知状态返回原值
        };
    }
}
