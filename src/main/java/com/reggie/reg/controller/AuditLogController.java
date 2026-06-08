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

    /**
     * 获取审核统计数据接口
     * @return 返回包含待审核、已通过、已拒绝数量的统计结果
     */
    @GetMapping("/stats")
    public R<AuditStatsVO> getAuditStats() {
        // 查询状态为"PENDING"（待审核）的审核日志数量
        long pending = auditLogService.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "PENDING"));
        // 查询状态为"PASS"（已通过）的审核日志数量
        long passed = auditLogService.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "PASS"));
        // 查询状态为"REJECT"（已拒绝）的审核日志数量
        long rejected = auditLogService.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "REJECT"));

        // 构建并返回包含审核统计数据的响应对象
        return R.success(new AuditStatsVO(pending, passed, rejected));
    }

    /**
     * 获取审核日志列表
     * @param targetType 目标类型（可选）
     * @param status 审核状态（可选）
     * @param keyword 关键词（可选）
     * @param page 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @return 返回分页后的审核日志列表
     */
    @GetMapping("/list")
    public R<Page<AuditLogVO>> listAudit(
            @RequestParam(required = false) String targetType,  // 目标类型参数
            @RequestParam(required = false) String status,       // 审核状态参数
            @RequestParam(required = false) String keyword,      // 关键词参数
            @RequestParam(defaultValue = "1") Integer page,     // 页码参数，默认为1
            @RequestParam(defaultValue = "10") Integer size) {  // 每页大小参数，默认为10

        // 1. 构建查询条件
        LambdaQueryWrapper<AuditLog> query = new LambdaQueryWrapper<>();
        query.eq(StringUtils.isNotBlank(targetType), AuditLog::getTargetType, targetType);  // 添加目标类型条件
        query.eq(StringUtils.isNotBlank(status), AuditLog::getResult, status);             // 添加审核状态条件
        query.like(StringUtils.isNotBlank(keyword), AuditLog::getReason, keyword);           // 添加关键词条件
        query.orderByDesc(AuditLog::getAuditTime);                                        // 按审核时间降序排序

        // 2. 执行分页查询
        Page<AuditLog> auditPage = auditLogService.page(new Page<>(page, size), query);     // 执行分页查询获取数据

        // 3. 转换为 VO 列表
        List<AuditLogVO> voList = auditPage.getRecords().stream()                         // 将实体列表转换为VO列表
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 4. 构建 VO 分页结果
        Page<AuditLogVO> voPage = new Page<>();                                           // 创建新的VO分页对象
        voPage.setRecords(voList);                                                       // 设置记录列表
        voPage.setTotal(auditPage.getTotal());                                           // 设置总记录数
        voPage.setSize(auditPage.getSize());                                             // 设置每页大小
        voPage.setCurrent(auditPage.getCurrent());                                       // 设置当前页码
        voPage.setPages(auditPage.getPages());                                          // 设置总页数

        return R.success(voPage);                                                       // 返回成功响应，包含分页数据
    }

    /**
     * 处理审核通过请求
     * @param auditId 审核记录ID
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回操作结果，包含成功或失败信息
     */
    @PostMapping("/{auditId}/approve")
    public R<String> approveAudit(@PathVariable Integer auditId, HttpServletRequest request) {
        // 1. 获取当前管理员信息，验证登录状态
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        if (adminId == null) {
            return R.error("未登录");
        }

        String role=(String)request.getSession().getAttribute("sys_user_role");
        if(!"ADMIN".equals(role)){
            return R.error("权限不足 无法进行此操作");
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

    /**
     * 处理拒绝审核的请求
     * @param auditId 审核记录ID
     * @param body 包含拒绝原因的请求体
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回操作结果，成功或失败信息
     */
    @PostMapping("/{auditId}/reject")
    public R<String> rejectAudit(@PathVariable Integer auditId,
                                 @RequestBody Map<String, String> body,
                                 HttpServletRequest request) {

        // 1. 获取参数
        String reason = body.get("reason"); // 从请求体中获取拒绝原因
        if (StringUtils.isBlank(reason)) { // 检查拒绝原因是否为空
            return R.error("拒绝原因不能为空");
        }

        // 2. 获取当前管理员
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user"); // 从会话中获取管理员ID
        log.info("{}",adminId); // 记录管理员ID日志
        if (adminId == null) { // 检查管理员是否登录
            return R.error("未登录");
        }

        // 3. 查询审核记录
        AuditLog auditLog = auditLogService.getById(auditId); // 根据ID获取审核记录
        if (auditLog == null) { // 检查审核记录是否存在
            return R.error("审核记录不存在");
        }
        if (!"PENDING".equals(auditLog.getResult())) { // 检查审核记录状态是否为待审核
            return R.error("只有待审核的内容才能拒绝");
        }

        // 4. 更新审核记录
        auditLog.setResult("REJECT"); // 设置审核结果为拒绝
        auditLog.setReason(reason); // 设置拒绝原因
        auditLog.setAuditorId(adminId); // 设置审核人ID
        auditLog.setAuditTime(LocalDateTime.now()); // 设置审核时间
        auditLogService.updateById(auditLog); // 更新审核记录

        // 5. 同步更新内容表状态
        updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "REJECT"); // 更新对应内容的审核状态

        return R.success("已拒绝"); // 返回成功信息
    }


    /**
     * 批量审核通过接口
     * @param body 包含auditIds键的请求体，值为要审核的ID列表
     * @param request HTTP请求对象，用于获取session中的用户信息
     * @return 返回操作结果，R<String>类型，包含成功或失败信息
     */
    @PostMapping("/batch/approve")
    public R<String> batchApprove(@RequestBody Map<String, List<Integer>> body,
                                  HttpServletRequest request) {
        // 从请求体中获取要审核的ID列表
        List<Integer> auditIds = body.get("auditIds");
        // 检查ID列表是否为空
        if (auditIds == null || auditIds.isEmpty()) {
            return R.error("请选择要审核的内容");
        }

        // 从session中获取管理员ID
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        // 检查是否登录
        if (adminId == null) {
            return R.error("未登录");
        }

        // 批量查询待审核记录
        List<AuditLog> auditLogs = auditLogService.list(new LambdaQueryWrapper<AuditLog>()
                .in(AuditLog::getAuditId, auditIds)  // 匹配指定的审核ID列表
                .eq(AuditLog::getResult, "PENDING")); // 只查询状态为"PENDING"的记录

        // 检查是否有可审核的记录
        if (auditLogs.isEmpty()) {
            return R.success("没有可审核的内容");
        }



        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 遍历审核记录，更新状态
        for (AuditLog auditLog : auditLogs) {
            auditLog.setResult("PASS");        // 设置审核结果为通过
            auditLog.setAuditorId(adminId);     // 设置审核人ID
            auditLog.setAuditTime(now);        // 设置审核时间
            // 同步更新内容状态
            updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "PASS");
        }
        auditLogService.updateBatchById(auditLogs);

        return R.success("批量通过成功");
    }

    /**
     * 批量拒绝审核请求
     * @param body 包含审核ID列表和拒绝原因的请求体
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回操作结果，成功或失败信息
     */
    @PostMapping("/batch/reject")
    public R<String> batchReject(@RequestBody Map<String, Object> body,
                                 HttpServletRequest request) {
        // 从请求体中获取审核ID列表和拒绝原因 黄的原因是没有进行类型检测 需要和前端进行类型匹配
        List<Integer> auditIds = (List<Integer>) body.get("auditIds");
        // 从前端获取批量拒绝的原因 只支持同一原因
        String reason = (String) body.get("reason");

        // 校验审核ID列表是否为空
        if (auditIds == null || auditIds.isEmpty()) {
            return R.error("请选择要拒绝的内容");
        }

        // 校验拒绝原因是否为空
        if (StringUtils.isBlank(reason)) {
            return R.error("拒绝原因不能为空");
        }

        // 从会话中获取管理员ID
        Integer adminId = (Integer) request.getSession().getAttribute("sys_user");
        // 校验管理员是否登录
        if (adminId == null) {
            return R.error("未登录");
        }
        String role = (String) request.getSession().getAttribute("sys_user_role");
        if (!"ADMIN".equals(role)) {
            return R.error("只有管理员才能进行审核操作");
        }

        // 批量查询待审核记录
        List<AuditLog> auditLogs = auditLogService.list(new LambdaQueryWrapper<AuditLog>()
                .in(AuditLog::getAuditId, auditIds)  // 匹配指定的审核ID列表
                .eq(AuditLog::getResult, "PENDING")); // 只查询状态为待审核的记录

        // 如果没有查询到待审核记录，返回提示信息
        if (auditLogs.isEmpty()) {
            return R.success("没有可审核的内容");
        }

        // 批量更新审核记录
        LocalDateTime now = LocalDateTime.now(); // 获取当前时间
        for (AuditLog auditLog : auditLogs) {
            // 更新审核结果为拒绝
            auditLog.setResult("REJECT");
            // 设置拒绝原因
            auditLog.setReason(reason);
            // 设置审核人ID
            auditLog.setAuditorId(adminId);
            // 设置审核时间
            auditLog.setAuditTime(now);
            // 同步更新内容状态
            updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "REJECT");
        }
        // 利用mybatis-plus进行更新
        auditLogService.updateBatchById(auditLogs);

        return R.success("批量拒绝成功");
    }

    /**
     * 重新审核接口
     * @param auditId 审核记录ID
     * @return 返回操作结果信息
     */
    @PostMapping("/{auditId}/reaudit")
    public R<String> reaudit(@PathVariable Integer auditId) {
        // 根据审核ID获取审核记录 由前端传入id
        AuditLog auditLog = auditLogService.getById(auditId);
        // 判断审核记录是否存在
        if (auditLog == null) {
            return R.error("审核记录不存在");
        }
        // 判断审核记录是否已经是待审核状态
        if ("PENDING".equals(auditLog.getResult())) {
            return R.error("该内容已是待审核状态");
        }

        // 重置审核记录相关信息
        auditLog.setResult("PENDING");     // 设置审核结果为待审核
        auditLog.setReason(null);          // 清空审核原因
        auditLog.setAuditorId(null);       // 清空审核人ID
        auditLog.setAuditTime(null);       // 清空审核时间
        // 更新审核记录到数据库
        auditLogService.updateById(auditLog);

        // 重置内容状态
        updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "PENDING");

        return R.success("已重置为待审核");
    }



   /**
    * 将审计日志实体对象转换为视图对象(VO)
    * @param log 审计日志实体对象
    * @return 转换后的审计日志视图对象，如果输入为null则返回null
    */
   private AuditLogVO convertToVO(AuditLog log) {
        // 如果输入参数为null，直接返回null
        if (log == null) return null;

        // 创建新的VO对象并复制基础属性
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
     * 组装资源视图对象(ResourceVO)
     * @param vo 审计日志视图对象
     * @param resourceId 资源ID
     */
    private void assembleResourceVO(AuditLogVO vo, Integer resourceId) {
        // 根据资源ID获取资源信息
        CourseResource resource = resourceService.getById(resourceId);
        // 如果资源不存在，则直接返回
        if (resource == null) return;

        // 创建资源简单视图对象
        ResourceSimpleVO simple = new ResourceSimpleVO();
        // 设置资源基本信息
        simple.setResourceId(resource.getResourceId());
        simple.setTitle(resource.getTitle());
        simple.setType(resource.getType());
        simple.setFileUrl(resource.getFileUrl());

        // 关联课程名称
        if (resource.getCourseId() != null) {
            // 根据课程ID获取课程信息
            CourseInfo course = courseService.getById(resource.getCourseId());
            // 如果课程存在，则设置课程名称
            if (course != null) {
                simple.setCourseName(course.getCourseName());
            }
        }

        // 将资源信息设置到审计日志VO中
        vo.setResource(simple);
        // 设置内容预览为资源标题
        vo.setContentPreview(resource.getTitle());
        // 设置提交时间，如果创建时间为空则为null
        vo.setSubmitTime(resource.getCreateTime() != null ?
                resource.getCreateTime().format(DT_FORMAT) : null);
    }


    /**
     * 组装问答相关数据到审核日志VO对象中
     * @param vo 审核日志VO对象
     * @param qaId 问答ID
     */
    private void assembleQaVO(AuditLogVO vo, Integer qaId) {
        // 根据ID获取问答信息
        CourseQa qa = qaService.getById(qaId);
        // 如果问答不存在，直接返回
        if (qa == null) return;

        // 创建简单问答VO对象
        QaSimpleVO simple = new QaSimpleVO();
        // 设置问答ID
        simple.setQaId(qa.getQaId());
        // 设置问题内容
        simple.setQuestion(qa.getQuestion());
        // 设置回答内容
        simple.setAnswer(qa.getAnswer());
        // 设置是否匿名
        simple.setIsAnonymous(qa.getIsAnonymous());
        // 设置提问时间，格式化为指定格式，如果为null则设为null
        simple.setAskTime(qa.getAskTime() != null ?
                qa.getAskTime().format(DT_FORMAT) : null);

        // 关联课程名称
        if (qa.getCourseId() != null) {
            // 根据课程ID获取课程信息
            CourseInfo course = courseService.getById(qa.getCourseId());
            // 如果课程存在，设置课程名称
            if (course != null) {
                simple.setCourseName(course.getCourseName());
            }
        }

        // 将组装好的问答信息设置到审核日志VO中
        vo.setQa(simple);
        // 设置内容预览，截取问题前30个字符
        vo.setContentPreview(truncateText(qa.getQuestion(), 30));
        // 设置提交时间为提问时间
        vo.setSubmitTime(simple.getAskTime());
    }


    /**
     * 组装提交记录的视图对象(VO)
     * @param vo 审计日志视图对象，用于填充提交相关信息
     * @param submissionId 提交记录ID，用于查询具体的提交信息
     */
    private void assembleSubmissionVO(AuditLogVO vo, Integer submissionId) {
        // 根据提交ID获取提交记录
        Submission submission = submissionService.getById(submissionId);
        // 如果提交记录不存在，直接返回
        if (submission == null) return;

        // 创建提交记录的简化视图对象
        SubmissionSimpleVO simple = new SubmissionSimpleVO();
        // 设置提交记录的基本属性
        simple.setSubmissionId(submission.getSubmissionId());
        simple.setContentType(submission.getContentType());
        simple.setFilePath(submission.getFilePath());
        simple.setTextContent(submission.getTextContent());
        simple.setIsLate(submission.getIsLate());
        // 设置分数，如果为null则设置为null
        simple.setScore(submission.getScore() != null ?
                submission.getScore().toString() : null);
        // 设置提交时间，如果为null则设置为null，否则格式化时间
        simple.setSubmitTime(submission.getSubmitTime() != null ?
                submission.getSubmitTime().format(DT_FORMAT) : null);

        // 关联作业标题
        // 如果提交记录有关联的作业ID
        if (submission.getAssignmentId() != null) {
            // 根据作业ID获取作业信息
            Assignment assignment = assignmentService.getById(submission.getAssignmentId());
            // 如果作业信息存在，设置作业标题
            if (assignment != null) {
                simple.setAssignmentTitle(assignment.getTitle());
            }
        }

        // 将处理后的提交信息设置到审计日志VO中
        vo.setSubmission(simple);
        // 设置内容预览为作业标题
        vo.setContentPreview(simple.getAssignmentTitle());
        // 根据不同的内容类型设置不同的内容预览

        // 设置提交时间
        vo.setSubmitTime(simple.getSubmitTime());
    }

    /**
     * 从审计日志中提取提交者ID
     * 根据日志的目标类型和目标ID，从相应的服务中获取资源并返回提交者ID
     *
     * @param log 审计日志对象，包含目标类型和目标ID等信息
     * @return 提交者ID，如果日志为空、目标ID为空或找不到对应资源则返回null
     */
    private Integer extractSubmitterId(AuditLog log) {
        // 检查日志或目标ID是否为空，如果为空则直接返回null
        if (log == null || log.getTargetId() == null) return null;

        // 如果目标类型为"RESOURCE"，则从资源服务中获取资源并返回上传者ID
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
     * 更新内容审核状态
     * 根据不同的目标类型（资源、问答、提交物）更新对应的审核状态
     * @param targetType 目标类型，可以是"RESOURCE"、"QA"或"SUBMISSION"
     * @param targetId 目标ID，对应具体内容的唯一标识
     * @param status 审核状态，用于更新内容的审核状态
     */
    private void updateContentAuditStatus(String targetType, Integer targetId, String status) {
        // 如果目标ID为空，直接返回不做任何操作
        if (targetId == null) return;

        // 如果是资源类型，更新课程资源的审核状态
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
