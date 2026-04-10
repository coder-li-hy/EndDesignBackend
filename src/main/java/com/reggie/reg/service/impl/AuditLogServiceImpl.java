package com.reggie.reg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.dto.*;
import com.reggie.reg.entity.*;
import com.reggie.reg.mapper.AuditLogMapper;
import com.reggie.reg.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 审核日志表 服务实现类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements IAuditLogService {
    private final ICourseResourceService resourceService;
    private final ICourseQaService qaService;
    private final ISubmissionService submissionService;
    private final ISysUserService sysUserService;
    private final ICourseInfoService courseService;
    private final IAssignmentService assignmentService;

    // ========== 查询接口实现 ==========

    @Override
    public AuditStatsVO getAuditStats() {
        // 使用 MyBatis-Plus 的 count 方法，避免全表扫描
        long pending = this.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "PENDING"));
        long passed = this.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "PASS"));
        long rejected = this.count(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getResult, "REJECT"));

        return new AuditStatsVO(pending, passed, rejected, pending + passed + rejected);
    }

    @Override
    public Page<AuditLogVO> pageAuditVO(Page<AuditLog> page, AuditQueryDTO query) {
        // 1. 构建查询条件
        LambdaQueryWrapper<AuditLog> queryWrapper = buildQueryWrapper(query);

        // 2. 执行分页查询
        Page<AuditLog> auditPage = this.page(page, queryWrapper);

        // 3. 批量转换 VO（关键：避免 N+1 查询）
        List<AuditLogVO> voList = convertBatchToVO(auditPage.getRecords());

        // 4. 构建 VO 分页结果
        Page<AuditLogVO> voPage = new Page<>();
        voPage.setRecords(voList);
        voPage.setTotal(auditPage.getTotal());
        voPage.setSize(auditPage.getSize());
        voPage.setCurrent(auditPage.getCurrent());
        voPage.setPages(auditPage.getPages());

        return voPage;
    }

    @Override
    public AuditLogDetailVO getDetail(Integer auditId) {
        AuditLog log = this.getById(auditId);
        if (log == null) {
            throw new RuntimeException("审核记录不存在");
        }
        return convertToDetailVO(log);
    }

    // ========== 审核操作接口实现 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Integer auditId, Integer adminId) {
        AuditLog auditLog = this.getById(auditId);
        if (auditLog == null) {
            throw new RuntimeException("审核记录不存在");
        }
        if (!"PENDING".equals(auditLog.getResult())) {
            throw new RuntimeException("只有待审核的内容才能通过");
        }

        // 1. 更新审核记录
        auditLog.setResult("PASS");
        auditLog.setAuditorId(adminId);
        auditLog.setAuditTime(LocalDateTime.now());
        this.updateById(auditLog);

        // 2. 更新对应内容的审核状态（关键：让内容可见）
        updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "PASS");

        log.info("审核通过: auditId={}, targetType={}, targetId={}, adminId={}",
                auditId, auditLog.getTargetType(), auditLog.getTargetId(), adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Integer auditId, Integer adminId, String reason) {
        if (StringUtils.isBlank(reason)) {
            throw new RuntimeException("拒绝原因不能为空");
        }

        AuditLog auditLog = this.getById(auditId);
        if (auditLog == null) {
            throw new RuntimeException("审核记录不存在");
        }
        if (!"PENDING".equals(auditLog.getResult())) {
            throw new RuntimeException("只有待审核的内容才能拒绝");
        }

        // 1. 更新审核记录
        auditLog.setResult("REJECT");
        auditLog.setReason(reason);
        auditLog.setAuditorId(adminId);
        auditLog.setAuditTime(LocalDateTime.now());
        this.updateById(auditLog);

        // 2. 更新对应内容的审核状态（关键：隐藏内容）
        updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "REJECT");

        // 3. TODO: 发送通知给用户（可选）
        // notificationService.sendAuditResult(...);

        log.info("审核拒绝: auditId={}, reason={}, adminId={}", auditId, reason, adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchApprove(List<Integer> auditIds, Integer adminId) {
        if (auditIds == null || auditIds.isEmpty()) {
            return;
        }

        // 批量查询待审核记录
        List<AuditLog> auditLogs = this.list(new LambdaQueryWrapper<AuditLog>()
                .in(AuditLog::getAuditId, auditIds)
                .eq(AuditLog::getResult, "PENDING"));

        if (auditLogs.isEmpty()) {
            return;
        }

        // 批量更新
        LocalDateTime now = LocalDateTime.now();
        for (AuditLog auditLog : auditLogs) {
            auditLog.setResult("PASS");
            auditLog.setAuditorId(adminId);
            auditLog.setAuditTime(now);

            // 更新内容状态
            updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "PASS");
        }
        this.updateBatchById(auditLogs);

        log.info("批量通过: count={}, adminId={}", auditLogs.size(), adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReject(List<Integer> auditIds, Integer adminId, String reason) {
        if (StringUtils.isBlank(reason)) {
            throw new RuntimeException("拒绝原因不能为空");
        }
        if (auditIds == null || auditIds.isEmpty()) {
            return;
        }

        // 批量查询待审核记录
        List<AuditLog> auditLogs = this.list(new LambdaQueryWrapper<AuditLog>()
                .in(AuditLog::getAuditId, auditIds)
                .eq(AuditLog::getResult, "PENDING"));

        if (auditLogs.isEmpty()) {
            return;
        }

        // 批量更新
        LocalDateTime now = LocalDateTime.now();
        for (AuditLog auditLog : auditLogs) {
            auditLog.setResult("REJECT");
            auditLog.setReason(reason);
            auditLog.setAuditorId(adminId);
            auditLog.setAuditTime(now);

            // 更新内容状态
            updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "REJECT");
        }
        this.updateBatchById(auditLogs);

        log.info("批量拒绝: count={}, adminId={}", auditLogs.size(), adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetToPending(Integer auditId) {
        AuditLog auditLog = this.getById(auditId);
        if (auditLog == null) {
            throw new RuntimeException("审核记录不存在");
        }

        // 只允许重置已审核的记录
        if ("PENDING".equals(auditLog.getResult())) {
            throw new RuntimeException("该内容已是待审核状态");
        }

        // 重置审核记录
        auditLog.setResult("PENDING");
        auditLog.setReason(null);
        auditLog.setAuditorId(null);
        auditLog.setAuditTime(null);
        this.updateById(auditLog);

        // 重置内容状态为待审核
        updateContentAuditStatus(auditLog.getTargetType(), auditLog.getTargetId(), "PENDING");

        log.info("重置为待审核: auditId={}", auditId);
    }

    // ========== VO 转换方法（核心）==========

    @Override
    public AuditLogVO convertToVO(AuditLog log) {
        if (log == null) return null;

        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(log, vo);

        // 设置中文文本
        vo.setTargetTypeText(getTargetTypeText(log.getTargetType()));
        vo.setResultText(getResultText(log.getResult()));

        // 根据类型关联查询内容摘要（只查必要字段）
        if ("RESOURCE".equals(log.getTargetType())) {
            assembleResourceSimpleVO(vo, log.getTargetId());
        } else if ("QA".equals(log.getTargetType())) {
            assembleQaSimpleVO(vo, log.getTargetId());
        } else if ("SUBMISSION".equals(log.getTargetType())) {
            assembleSubmissionSimpleVO(vo, log.getTargetId());
        }

        return vo;
    }

    @Override
    public AuditLogDetailVO convertToDetailVO(AuditLog log) {
        if (log == null) return null;

        // 先转为基础 VO
        AuditLogDetailVO detail = new AuditLogDetailVO();
        BeanUtils.copyProperties(log, detail);
        detail.setTargetTypeText(getTargetTypeText(log.getTargetType()));
        detail.setResultText(getResultText(log.getResult()));

        // 根据类型组装完整详情
        if ("RESOURCE".equals(log.getTargetType())) {
            assembleResourceDetailVO(detail, log.getTargetId());
        } else if ("QA".equals(log.getTargetType())) {
            assembleQaDetailVO(detail, log.getTargetId());
        } else if ("SUBMISSION".equals(log.getTargetType())) {
            assembleSubmissionDetailVO(detail, log.getTargetId());
        }

        // 组装完整的提交者/审核人信息
        assembleSubmitterDetailVO(detail, log);
        assembleAuditorDetailVO(detail, log);

        // 组装审核操作历史（如果需要）
        // detail.setOperationHistory(getOperationHistory(log.getAuditId()));

        return detail;
    }

    /**
     * 批量转换 VO（性能优化：避免 N+1 查询）
     */
    private List<AuditLogVO> convertBatchToVO(List<AuditLog> logs) {
        if (logs == null || logs.isEmpty()) return Collections.emptyList();

        // 1. 先转为基础 VO（不含关联数据）
        List<AuditLogVO> voList = logs.stream()
                .map(log -> {
                    AuditLogVO vo = new AuditLogVO();
                    BeanUtils.copyProperties(log, vo);
                    vo.setTargetTypeText(getTargetTypeText(log.getTargetType()));
                    vo.setResultText(getResultText(log.getResult()));
                    return vo;
                })
                .collect(Collectors.toList());

        // 2. 批量查询关联数据（按类型分组）
        batchAssembleResources(voList);
        batchAssembleQas(voList);
        batchAssembleSubmissions(voList);
        batchAssembleUsers(voList);

        return voList;
    }
    /**
     * 批量组装作业提交信息（避免 N+1 查询）
     */
    private void batchAssembleSubmissions(List<AuditLogVO> voList) {
        // 1. 收集所有 SUBMISSION 类型的 targetId
        List<Integer> submissionIds = voList.stream()
                .filter(vo -> "SUBMISSION".equals(vo.getTargetType()) && vo.getTargetId() != null)
                .map(AuditLogVO::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        if (submissionIds.isEmpty()) return;

        // 2. 批量查询作业提交记录
        Map<Integer, Submission> submissionMap = submissionService.listByIds(submissionIds)
                .stream().collect(Collectors.toMap(Submission::getSubmissionId, s -> s));

        // 3. 批量查询关联的作业标题
        List<Integer> assignmentIds = submissionMap.values().stream()
                .map(Submission::getAssignmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> assignmentTitleMap = new HashMap<>();
        if (!assignmentIds.isEmpty()) {
            assignmentTitleMap = assignmentService.listByIds(assignmentIds)
                    .stream().collect(Collectors.toMap(Assignment::getAssignmentId, Assignment::getTitle));
        }

        // 4. 填充到 VO
        for (AuditLogVO vo : voList) {
            if ("SUBMISSION".equals(vo.getTargetType()) && submissionMap.containsKey(vo.getTargetId())) {
                Submission s = submissionMap.get(vo.getTargetId());

                SubmissionSimpleVO simple = new SubmissionSimpleVO();
                simple.setSubmissionId(s.getSubmissionId());
                simple.setContentType(s.getContentType());
                simple.setFilePath(s.getFilePath());
                simple.setTextContent(s.getTextContent());
                simple.setIsLate(s.getIsLate());
                simple.setScore(s.getScore());
                simple.setSubmitTime(s.getSubmitTime());

                // 设置作业标题
                if (s.getAssignmentId() != null && assignmentTitleMap.containsKey(s.getAssignmentId())) {
                    simple.setAssignmentTitle(assignmentTitleMap.get(s.getAssignmentId()));
                }

                vo.setSubmission(simple);
                vo.setContentPreview(simple.getAssignmentTitle());
                vo.setSubmitTime(s.getSubmitTime());
            }
        }
    }

    /**
     * 批量组装问答信息（避免 N+1 查询）
     */
    private void batchAssembleQas(List<AuditLogVO> voList) {
        // 1. 收集所有 QA 类型的 targetId
        List<Integer> qaIds = voList.stream()
                .filter(vo -> "QA".equals(vo.getTargetType()) && vo.getTargetId() != null)
                .map(AuditLogVO::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        if (qaIds.isEmpty()) return;

        // 2. 批量查询问答记录
        Map<Integer, CourseQa> qaMap = qaService.listByIds(qaIds)
                .stream().collect(Collectors.toMap(CourseQa::getQaId, q -> q));

        // 3. 批量查询关联的课程名称
        List<Integer> courseIds = qaMap.values().stream()
                .map(CourseQa::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> courseNameMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseNameMap = courseService.listByIds(courseIds)
                    .stream().collect(Collectors.toMap(CourseInfo::getCourseId, CourseInfo::getCourseName));
        }

        // 4. 填充到 VO
        for (AuditLogVO vo : voList) {
            if ("QA".equals(vo.getTargetType()) && qaMap.containsKey(vo.getTargetId())) {
                CourseQa qa = qaMap.get(vo.getTargetId());

                QaSimpleVO simple = new QaSimpleVO();
                simple.setQaId(qa.getQaId());
                simple.setQuestion(qa.getQuestion());
                simple.setAnswer(qa.getAnswer());
                simple.setIsAnonymous(qa.getIsAnonymous());
                simple.setAskTime(qa.getAskTime());

                // 设置课程名称
                if (qa.getCourseId() != null && courseNameMap.containsKey(qa.getCourseId())) {
                    simple.setCourseName(courseNameMap.get(qa.getCourseId()));
                }

                vo.setQa(simple);
                vo.setContentPreview(truncateText(qa.getQuestion(), 30));
                vo.setSubmitTime(qa.getAskTime());
            }
        }
    }

    // ========== 内容组装方法（列表用 - SimpleVO）==========

    private void assembleResourceSimpleVO(AuditLogVO vo, Integer resourceId) {
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
        vo.setSubmitTime(resource.getCreateTime());
    }

    private void assembleQaSimpleVO(AuditLogVO vo, Integer qaId) {
        CourseQa qa = qaService.getById(qaId);
        if (qa == null) return;

        QaSimpleVO simple = new QaSimpleVO();
        simple.setQaId(qa.getQaId());
        simple.setQuestion(qa.getQuestion());
        simple.setAnswer(qa.getAnswer());
        simple.setIsAnonymous(qa.getIsAnonymous());
        simple.setAskTime(qa.getAskTime());

        // 关联课程名称
        if (qa.getCourseId() != null) {
            CourseInfo course = courseService.getById(qa.getCourseId());
            if (course != null) {
                simple.setCourseName(course.getCourseName());
            }
        }

        vo.setQa(simple);
        vo.setContentPreview(truncateText(qa.getQuestion(), 30));
        vo.setSubmitTime(qa.getAskTime());
    }

    private void assembleSubmissionSimpleVO(AuditLogVO vo, Integer submissionId) {
        Submission submission = submissionService.getById(submissionId);
        if (submission == null) return;

        SubmissionSimpleVO simple = new SubmissionSimpleVO();
        simple.setSubmissionId(submission.getSubmissionId());
        simple.setContentType(submission.getContentType());
        simple.setFilePath(submission.getFilePath());
        simple.setTextContent(submission.getTextContent());
        simple.setIsLate(submission.getIsLate());
        simple.setScore(submission.getScore());
        simple.setSubmitTime(submission.getSubmitTime());

        // 关联作业标题
        if (submission.getAssignmentId() != null) {
            Assignment assignment = assignmentService.getById(submission.getAssignmentId());
            if (assignment != null) {
                simple.setAssignmentTitle(assignment.getTitle());
            }
        }

        vo.setSubmission(simple);
        vo.setContentPreview(simple.getAssignmentTitle());
        vo.setSubmitTime(submission.getSubmitTime());
    }

    // ========== 内容组装方法（详情用 - DetailVO）==========

    private void assembleResourceDetailVO(AuditLogDetailVO detail, Integer resourceId) {
        CourseResource resource = resourceService.getById(resourceId);
        if (resource == null) return;

        ResourceDetailVO detailVO = new ResourceDetailVO();
        BeanUtils.copyProperties(resource, detailVO);

        // 关联课程
        if (resource.getCourseId() != null) {
            CourseInfo course = courseService.getById(resource.getCourseId());
            if (course != null) {
                detailVO.setCourse(convertToCourseSimpleVO(course));
                detail.setCourseInfo(detailVO.getCourse());
            }
        }

        // 关联上传教师
        if (resource.getUploaderId() != null) {
            SysUser uploader = sysUserService.getById(resource.getUploaderId());
            if (uploader != null) {
                detailVO.setUploader(convertToUserDetailVO(uploader));
            }
        }

        detail.setResourceDetail(detailVO);
        detail.setFullContent(resource.getTitle());
    }

    private void assembleQaDetailVO(AuditLogDetailVO detail, Integer qaId) {
        CourseQa qa = qaService.getById(qaId);
        if (qa == null) return;

        QaDetailVO detailVO = new QaDetailVO();
        BeanUtils.copyProperties(qa, detailVO);

        // 关联课程
        if (qa.getCourseId() != null) {
            CourseInfo course = courseService.getById(qa.getCourseId());
            if (course != null) {
                detailVO.setCourse(convertToCourseSimpleVO(course));
                detail.setCourseInfo(detailVO.getCourse());
            }
        }

        // 关联学生（提问者）
        if (qa.getStudentId() != null && !qa.getIsAnonymous()) {
            SysUser student = sysUserService.getById(qa.getStudentId());
            if (student != null) {
                detailVO.setStudent(convertToUserDetailVO(student));
            }
        }

        // 关联教师（回答者）
        if (qa.getTeacherId() != null && qa.getAnswer() != null) {
            SysUser teacher = sysUserService.getById(qa.getTeacherId());
            if (teacher != null) {
                detailVO.setTeacher(convertToUserDetailVO(teacher));
            }
        }

        detail.setQaDetail(detailVO);
        detail.setFullContent(qa.getQuestion());
    }

    private void assembleSubmissionDetailVO(AuditLogDetailVO detail, Integer submissionId) {
        Submission submission = submissionService.getById(submissionId);
        if (submission == null) return;

        SubmissionDetailVO detailVO = new SubmissionDetailVO();
        BeanUtils.copyProperties(submission, detailVO);

        // 关联作业
        if (submission.getAssignmentId() != null) {
            Assignment assignment = assignmentService.getById(submission.getAssignmentId());
            if (assignment != null) {
                detailVO.setAssignment(convertToAssignmentDetailVO(assignment));
            }
        }

        // 关联学生
        if (submission.getStudentId() != null) {
            SysUser student = sysUserService.getById(submission.getStudentId());
            if (student != null) {
                detailVO.setStudent(convertToUserDetailVO(student));
            }
        }

        detail.setSubmissionDetail(detailVO);
        detail.setFullContent(submission.getTextContent());
    }

    // ========== 用户信息组装 ==========

    private void assembleSubmitterDetailVO(AuditLogDetailVO detail, AuditLog log) {
        Integer submitterId = extractSubmitterId(log);
        if (submitterId != null) {
            SysUser submitter = sysUserService.getById(submitterId);
            if (submitter != null) {
                detail.setSubmitterDetail(convertToUserDetailVO(submitter));
            }
        }
    }

    private void assembleAuditorDetailVO(AuditLogDetailVO detail, AuditLog log) {
        if (log.getAuditorId() != null) {
            SysUser auditor = sysUserService.getById(log.getAuditorId());
            if (auditor != null) {
                detail.setAuditorDetail(convertToUserDetailVO(auditor));
            }
        }
    }

    // ========== 批量组装优化（避免 N+1）==========

    private void batchAssembleResources(List<AuditLogVO> voList) {
        // 收集所有 RESOURCE 类型的 targetId
        List<Integer> resourceIds = voList.stream()
                .filter(vo -> "RESOURCE".equals(vo.getTargetType()) && vo.getTargetId() != null)
                .map(AuditLogVO::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        if (resourceIds.isEmpty()) return;

        // 批量查询资源
        Map<Integer, CourseResource> resourceMap = resourceService.listByIds(resourceIds)
                .stream().collect(Collectors.toMap(CourseResource::getResourceId, r -> r));

        // 批量查询课程名称
        List<Integer> courseIds = resourceMap.values().stream()
                .map(CourseResource::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> courseNameMap = courseService.listByIds(courseIds)
                .stream().collect(Collectors.toMap(CourseInfo::getCourseId, CourseInfo::getCourseName));

        // 填充到 VO
        for (AuditLogVO vo : voList) {
            if ("RESOURCE".equals(vo.getTargetType()) && resourceMap.containsKey(vo.getTargetId())) {
                CourseResource r = resourceMap.get(vo.getTargetId());
                ResourceSimpleVO simple = new ResourceSimpleVO();
                BeanUtils.copyProperties(r, simple);
                if (r.getCourseId() != null && courseNameMap.containsKey(r.getCourseId())) {
                    simple.setCourseName(courseNameMap.get(r.getCourseId()));
                }
                vo.setResource(simple);
                vo.setContentPreview(r.getTitle());
                vo.setSubmitTime(r.getCreateTime());
            }
        }
    }

    // batchAssembleQas / batchAssembleSubmissions 类似...

    private void batchAssembleUsers(List<AuditLogVO> voList) {
        // 收集所有需要查询的用户 ID（提交者 + 审核人）
        Set<Integer> userIds = new HashSet<>();
        for (AuditLogVO vo : voList) {
            Integer submitterId = extractSubmitterId(vo);
            if (submitterId != null) userIds.add(submitterId);
            if (vo.getAuditorId() != null) userIds.add(vo.getAuditorId());
        }

        if (userIds.isEmpty()) return;

        // 批量查询用户
        Map<Integer, SysUser> userMap = sysUserService.listByIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getUserId, u -> u));

        // 填充到 VO
        for (AuditLogVO vo : voList) {
            Integer submitterId = extractSubmitterId(vo);
            if (submitterId != null && userMap.containsKey(submitterId)) {
                vo.setSubmitter(convertToUserSimpleVO(userMap.get(submitterId)));
            }
            if (vo.getAuditorId() != null && userMap.containsKey(vo.getAuditorId())) {
                vo.setAuditor(convertToUserSimpleVO(userMap.get(vo.getAuditorId())));
            }
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<AuditLog> buildQueryWrapper(AuditQueryDTO query) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(query.getTargetType())) {
            wrapper.eq(AuditLog::getTargetType, query.getTargetType());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AuditLog::getResult, query.getStatus());
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(w -> w
                    .like(AuditLog::getReason, query.getKeyword())
                    .or()
                    .like(AuditLog::getTargetType, query.getKeyword())
            );
        }

        wrapper.orderByDesc(AuditLog::getAuditTime);
        return wrapper;
    }

    /**
     * 更新对应内容的审核状态
     */
    private void updateContentAuditStatus(String targetType, Integer targetId, String status) {
        if (targetId == null) return;

        if ("RESOURCE".equals(targetType)) {
            CourseResource resource = new CourseResource();
            resource.setResourceId(targetId);
            resource.setAuditStatus(status);
            resourceService.updateById(resource);
        } else if ("QA".equals(targetType)) {
            // 问答表可能没有 audit_status 字段，根据实际设计调整
            // CourseQa qa = new CourseQa();
            // qa.setQaId(targetId);
            // qa.setAuditStatus(status);
            // qaService.updateById(qa);
        } else if ("SUBMISSION".equals(targetType)) {
            // 作业提交表可能没有 audit_status 字段，根据实际设计调整
        }
    }

    /**
     * 提取提交者 ID（根据内容类型）
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
     * 用户实体转详情 VO
     */
    private UserDetailVO convertToUserDetailVO(SysUser user) {
        if (user == null) return null;
        UserDetailVO detail = new UserDetailVO();
        BeanUtils.copyProperties(user, detail);
        detail.setRoleText(getRoleText(user.getRole()));
        detail.setStatusText(getStatusText(user.getStatus()));
        return detail;
    }

    /**
     * 课程实体转简化 VO
     */
    private CourseSimpleVO convertToCourseSimpleVO(CourseInfo course) {
        if (course == null) return null;
        CourseSimpleVO simple = new CourseSimpleVO();
        BeanUtils.copyProperties(course, simple);
        simple.setStatusText(getCourseStatusText(course.getStatus()));
        if (course.getTeacherId() != null) {
            SysUser teacher = sysUserService.getById(course.getTeacherId());
            if (teacher != null) {
                simple.setTeacher(convertToUserSimpleVO(teacher));
            }
        }
        return simple;
    }

    /**
     * 作业实体转详情 VO
     */
    private AssignmentDetailVO convertToAssignmentDetailVO(Assignment assignment) {
        if (assignment == null) return null;
        AssignmentDetailVO detail = new AssignmentDetailVO();
        BeanUtils.copyProperties(assignment, detail);
        if (assignment.getCourseId() != null) {
            CourseInfo course = courseService.getById(assignment.getCourseId());
            if (course != null) {
                detail.setCourse(convertToCourseSimpleVO(course));
            }
        }
        return detail;
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

    private String getStatusText(String status) {
        return switch (status) {
            case "ACTIVE" -> "正常";
            case "DISABLED" -> "禁用";
            default -> status;
        };
    }

    private String getCourseStatusText(String status) {
        return switch (status) {
            case "OPEN" -> "开放中";
            case "CLOSED" -> "已结课";
            case "ENDED" -> "已结束";
            default -> status;
        };
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

}
