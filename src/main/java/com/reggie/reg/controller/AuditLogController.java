package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.*;
import com.reggie.reg.service.*;
import com.reggie.reg.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 审核日志表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-10
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final IAuditLogService auditLogService;
    private final ICourseResourceService resourceService;
    private final ICourseQaService qaService;
    private final ISubmissionService submissionService;
    private final ISysUserService sysUserService;
    private final ICourseInfoService courseService;
    private final IAssignmentService assignmentService;

    // 日期格式化
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========== 1. 获取审核统计 ==========
    @GetMapping("/stats")
    public R<AuditStatsVO> getAuditStats() {
        long pending = auditLogService.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "PENDING"));
        long passed = auditLogService.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "PASS"));
        long rejected = auditLogService.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "REJECT"));

        return R.success(new AuditStatsVO(pending, passed, rejected));
    }

    // ========== 2. 分页查询审核列表 ==========
    @GetMapping("/list")
    public R<Page<AuditLogVO>> listAudit(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        // 1. 构建查询条件
        LambdaQueryWrapper<AuditLog> query = new LambdaQueryWrapper<>();
        query.eq(StringUtils.isNotBlank(targetType), AuditLog::getTargetType, targetType);
        query.eq(StringUtils.isNotBlank(status), AuditLog::getResult, status);
        query.like(StringUtils.isNotBlank(keyword), AuditLog::getReason, keyword);
        query.orderByDesc(AuditLog::getAuditTime);

        // 2. 执行分页查询
        Page<AuditLog> auditPage = auditLogService.page(new Page<>(page, size), query);

        // 3. 转换为 VO 列表
        List<AuditLogVO> voList = auditPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 4. 构建 VO 分页结果
        Page<AuditLogVO> voPage = new Page<>();
        voPage.setRecords(voList);
        voPage.setTotal(auditPage.getTotal());
        voPage.setSize(auditPage.getSize());
        voPage.setCurrent(auditPage.getCurrent());
        voPage.setPages(auditPage.getPages());

        return R.success(voPage);
    }

    // ========== 3. 通过审核（单个） ==========
    @PostMapping("/{auditId}/approve")
    public R<String> approveAudit(@PathVariable Integer auditId, HttpServletRequest request) {
        // 1. 获取当前管理员
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        if (adminId == null) {
            return R.error("未登录");
        }

        // 2. 查询审核记录
        AuditLog auditLog = auditLogService.getById(auditId);
        if (auditLog == null) {
            return R.error("审核记录不存在");
        }
        if (!(("PENDING").equals(auditLog.getResult()))) {
            return R.error("只有待审核的内容才能通过");
        }

        // 3. 更新审核记录
        auditLog.setResult("PASS");
        auditLog.setAuditorId(adminId);
        auditLog.setAuditTime(LocalDateTime.now());
        auditLogService.updateById(auditLog);

        // 4. 同步更新内容表状态（关键！）
        updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "PASS");

        return R.success("审核通过");
    }

    // ========== 4. 拒绝审核（单个） ==========
    @PostMapping("/{auditId}/reject")
    public R<String> rejectAudit(@PathVariable Integer auditId,
                                 @RequestBody Map<String, String> body,
                                 HttpServletRequest request) {
        // 1. 获取参数
        String reason = body.get("reason");
        if (StringUtils.isBlank(reason)) {
            return R.error("拒绝原因不能为空");
        }

        // 2. 获取当前管理员
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        log.info("{}",adminId);
        if (adminId == null) {
            return R.error("未登录");
        }

        // 3. 查询审核记录
        AuditLog auditLog = auditLogService.getById(auditId);
        if (auditLog == null) {
            return R.error("审核记录不存在");
        }
        if (!"PENDING".equals(auditLog.getResult())) {
            return R.error("只有待审核的内容才能拒绝");
        }

        // 4. 更新审核记录
        auditLog.setResult("REJECT");
        auditLog.setReason(reason);
        auditLog.setAuditorId(adminId);
        auditLog.setAuditTime(LocalDateTime.now());
        auditLogService.updateById(auditLog);

        // 5. 同步更新内容表状态
        updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "REJECT");

        return R.success("已拒绝");
    }

    // ========== 5. 批量通过 ==========
    @PostMapping("/batch/approve")
    public R<String> batchApprove(@RequestBody Map<String, List<Integer>> body,
                                  HttpServletRequest request) {
        List<Integer> auditIds = body.get("auditIds");
        if (auditIds == null || auditIds.isEmpty()) {
            return R.error("请选择要审核的内容");
        }

        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        if (adminId == null) {
            return R.error("未登录");
        }

        // 批量查询待审核记录
        List<AuditLog> auditLogs = auditLogService.list(new LambdaQueryWrapper<AuditLog>()
                .in(AuditLog::getAuditId, auditIds)
                .eq(AuditLog::getResult, "PENDING"));

        if (auditLogs.isEmpty()) {
            return R.success("没有可审核的内容");
        }

        // 批量更新
        LocalDateTime now = LocalDateTime.now();
        for (AuditLog auditLog : auditLogs) {
            auditLog.setResult("PASS");
            auditLog.setAuditorId(adminId);
            auditLog.setAuditTime(now);
            // 同步更新内容状态
            updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "PASS");
        }
        auditLogService.updateBatchById(auditLogs);

        return R.success("批量通过成功");
    }

    // ========== 6. 批量拒绝 ==========
    @PostMapping("/batch/reject")
    public R<String> batchReject(@RequestBody Map<String, Object> body,
                                 HttpServletRequest request) {
        List<Integer> auditIds = (List<Integer>) body.get("auditIds");
        String reason = (String) body.get("reason");

        if (auditIds == null || auditIds.isEmpty()) {
            return R.error("请选择要拒绝的内容");
        }
        if (StringUtils.isBlank(reason)) {
            return R.error("拒绝原因不能为空");
        }

        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        if (adminId == null) {
            return R.error("未登录");
        }

        // 批量查询待审核记录
        List<AuditLog> auditLogs = auditLogService.list(new LambdaQueryWrapper<AuditLog>()
                .in(AuditLog::getAuditId, auditIds)
                .eq(AuditLog::getResult, "PENDING"));

        if (auditLogs.isEmpty()) {
            return R.success("没有可审核的内容");
        }

        // 批量更新
        LocalDateTime now = LocalDateTime.now();
        for (AuditLog auditLog : auditLogs) {
            auditLog.setResult("REJECT");
            auditLog.setReason(reason);
            auditLog.setAuditorId(adminId);
            auditLog.setAuditTime(now);
            // 同步更新内容状态
            updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "REJECT");
        }
        auditLogService.updateBatchById(auditLogs);

        return R.success("批量拒绝成功");
    }

    // ========== 7. 重审（重置为待审核） ==========
    @PostMapping("/{auditId}/reaudit")
    public R<String> reaudit(@PathVariable Integer auditId) {
        AuditLog auditLog = auditLogService.getById(auditId);
        if (auditLog == null) {
            return R.error("审核记录不存在");
        }
        if ("PENDING".equals(auditLog.getResult())) {
            return R.error("该内容已是待审核状态");
        }

        // 重置审核记录
        auditLog.setResult("PENDING");
        auditLog.setReason(null);
        auditLog.setAuditorId(null);
        auditLog.setAuditTime(null);
        auditLogService.updateById(auditLog);

        // 重置内容状态
        updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "PENDING");

        return R.success("已重置为待审核");
    }

    // ========== 内部辅助方法 ==========

    /**
     * AuditLog 转 AuditLogVO（列表用）
     */
    private AuditLogVO convertToVO(AuditLog log) {
        if (log == null) return null;

        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(log, vo);

        // 格式化时间
        if (log.getAuditTime() != null) {
            vo.setAuditTime(log.getAuditTime().format(DT_FORMAT));
        }

        // 设置中文文本
        vo.setTargetTypeText(getTargetTypeText(log.getTargetType()));
        vo.setResultText(getResultText(log.getResult()));

        // 根据类型关联查询内容摘要
        if ("RESOURCE".equals(log.getTargetType())) {
            assembleResourceVO(vo, log.getTargetId());
        } else if ("QA".equals(log.getTargetType())) {
            assembleQaVO(vo, log.getTargetId());
        } else if ("SUBMISSION".equals(log.getTargetType())) {
            assembleSubmissionVO(vo, log.getTargetId());
        }

        // 查询提交者信息
        Integer submitterId = extractSubmitterId(log);
        if (submitterId != null) {
            SysUser submitter = sysUserService.getById(submitterId);
            if (submitter != null) {
                vo.setSubmitter(convertToUserSimpleVO(submitter));
            }
        }

        // 查询审核人信息
        if (log.getAuditorId() != null) {
            SysUser auditor = sysUserService.getById(log.getAuditorId());
            if (auditor != null) {
                vo.setAuditor(convertToUserSimpleVO(auditor));
            }
        }

        return vo;
    }

    /**
     * 组装资源信息
     */
    private void assembleResourceVO(AuditLogVO vo, Integer resourceId) {
        CourseResource resource = resourceService.getById(resourceId);
        if (resource == null) return;

        ResourceSimpleVO simple = new ResourceSimpleVO();
        simple.setResourceId(resource.getResourceId());
        simple.setTitle(resource.getTitle());
        simple.setType(resource.getType());
        simple.setFileUrl(resource.getFileUrl());

        // 关联课程名称
        if (resource.getCourseId() != null) {
            CourseInfo course = courseService.getById(resource.getCourseId());
            if (course != null) {
                simple.setCourseName(course.getCourseName());
            }
        }

        vo.setResource(simple);
        vo.setContentPreview(resource.getTitle());
        vo.setSubmitTime(resource.getCreateTime() != null ?
                resource.getCreateTime().format(DT_FORMAT) : null);
    }

    /**
     * 组装问答信息
     */
    private void assembleQaVO(AuditLogVO vo, Integer qaId) {
        CourseQa qa = qaService.getById(qaId);
        if (qa == null) return;

        QaSimpleVO simple = new QaSimpleVO();
        simple.setQaId(qa.getQaId());
        simple.setQuestion(qa.getQuestion());
        simple.setAnswer(qa.getAnswer());
        simple.setIsAnonymous(qa.getIsAnonymous());
        simple.setAskTime(qa.getAskTime() != null ?
                qa.getAskTime().format(DT_FORMAT) : null);

        // 关联课程名称
        if (qa.getCourseId() != null) {
            CourseInfo course = courseService.getById(qa.getCourseId());
            if (course != null) {
                simple.setCourseName(course.getCourseName());
            }
        }

        vo.setQa(simple);
        vo.setContentPreview(truncateText(qa.getQuestion(), 30));
        vo.setSubmitTime(simple.getAskTime());
    }

    /**
     * 组装作业提交信息
     */
    private void assembleSubmissionVO(AuditLogVO vo, Integer submissionId) {
        Submission submission = submissionService.getById(submissionId);
        if (submission == null) return;

        SubmissionSimpleVO simple = new SubmissionSimpleVO();
        simple.setSubmissionId(submission.getSubmissionId());
        simple.setContentType(submission.getContentType());
        simple.setFilePath(submission.getFilePath());
        simple.setTextContent(submission.getTextContent());
        simple.setIsLate(submission.getIsLate());
        simple.setScore(submission.getScore() != null ?
                submission.getScore().toString() : null);
        simple.setSubmitTime(submission.getSubmitTime() != null ?
                submission.getSubmitTime().format(DT_FORMAT) : null);

        // 关联作业标题
        if (submission.getAssignmentId() != null) {
            Assignment assignment = assignmentService.getById(submission.getAssignmentId());
            if (assignment != null) {
                simple.setAssignmentTitle(assignment.getTitle());
            }
        }

        vo.setSubmission(simple);
        vo.setContentPreview(simple.getAssignmentTitle());
        vo.setSubmitTime(simple.getSubmitTime());
    }

    /**
     * 提取提交者 ID
     */
    private Integer extractSubmitterId(AuditLog log) {
        if (log == null || log.getTargetId() == null) return null;

        if ("RESOURCE".equals(log.getTargetType())) {
            CourseResource r = resourceService.getById(log.getTargetId());
            return r != null ? r.getUploaderId() : null;
        } else if ("QA".equals(log.getTargetType())) {
            CourseQa q = qaService.getById(log.getTargetId());
            return q != null ? q.getStudentId() : null;
        } else if ("SUBMISSION".equals(log.getTargetType())) {
            Submission s = submissionService.getById(log.getTargetId());
            return s != null ? s.getStudentId() : null;
        }
        return null;
    }

    /**
     * 用户实体转简化 VO
     */
    private UserSimpleVO convertToUserSimpleVO(SysUser user) {
        if (user == null) return null;
        UserSimpleVO simple = new UserSimpleVO();
        simple.setUserId(user.getUserId());
        simple.setUsername(user.getUsername());
        simple.setRole(user.getRole());
        simple.setRoleText(getRoleText(user.getRole()));
        return simple;
    }

    /**
     * 同步更新内容表的审核状态
     */
    private void updateContentAuditStatus(String targetType, Integer targetId, String status) {
        if (targetId == null) return;

        if ("RESOURCE".equals(targetType)) {
            CourseResource resource = new CourseResource();
            resource.setResourceId(targetId);
            resource.setAuditStatus(status);
            resourceService.updateById(resource);
        } else if ("QA".equals(targetType)) {
            CourseQa qa = new CourseQa();
            qa.setQaId(targetId);
            qa.setAuditStatus(status);
            qaService.updateById(qa);
        } else if ("SUBMISSION".equals(targetType)) {
            Submission submission = new Submission();
            submission.setSubmissionId(targetId);
            submission.setAuditStatus(status);
            submissionService.updateById(submission);
        }
    }

    // ========== 文本转换工具 ==========

    private String getTargetTypeText(String type) {
        return switch (type) {
            case "RESOURCE" -> "课程资源";
            case "QA" -> "问答互动";
            case "SUBMISSION" -> "作业提交";
            default -> type;
        };
    }

    private String getResultText(String result) {
        return switch (result) {
            case "PENDING" -> "待审核";
            case "PASS" -> "已通过";
            case "REJECT" -> "已拒绝";
            default -> result;
        };
    }

    private String getRoleText(String role) {
        return switch (role) {
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学生";
            default -> role;
        };
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
