package com.reggie.reg.service.impl;

import com.reggie.reg.entity.AuditLog;
import com.reggie.reg.entity.Submission;
import com.reggie.reg.mapper.SubmissionMapper;
import com.reggie.reg.service.IAssignmentService;
import com.reggie.reg.service.IAuditLogService;
import com.reggie.reg.service.ISubmissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 作业提交表 服务实现类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl extends ServiceImpl<SubmissionMapper, Submission> implements ISubmissionService {
    private final IAuditLogService auditLogService;
     private final IAssignmentService assignmentService;

    /**
     * 提交作业的方法
     * @param submission 包含作业提交信息的对象
     * @return 提交是否成功，成功返回true，失败返回false
     * @Transactional 确保方法内所有数据库操作在一个事务中执行，遇到任何异常都会回滚
     * @rollbackFor = Exception.class 指定所有异常都触发回滚
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean submitAssignment(Submission submission) {
        // 1. 设置初始状态
        submission.setAuditStatus("PENDING");  // 设置审核状态为"待审核"
        submission.setSubmitTime(LocalDateTime.now());  // 设置提交时间为当前时间

        // 查询对应的Assignment的deadline
        // 通过assignmentService获取作业对象，并获取其截止时间
        LocalDateTime deadline = assignmentService.getById(submission.getAssignmentId()).getDeadline();

        // 2. 判断是否迟交（业务逻辑）
        // 比较提交时间和截止时间，如果提交时间晚于截止时间，则标记为迟交
        if (submission.getSubmitTime().isAfter(deadline)) {
            submission.setIsLate(true);
        }

        // 3. 保存提交
        // 调用save方法保存提交信息，如果保存失败则直接返回false
        boolean saved = this.save(submission);
        if (!saved) return false;

        // 4. 创建审核记录
        // 创建一条审核记录，记录类型为"SUBMISSION"，关联提交ID和学生ID
        createAuditLog("SUBMISSION", submission.getSubmissionId(), submission.getStudentId());

        return true;  // 提交成功返回true
    }

    /**
     * 创建审计日志的方法
     * @param targetType 目标类型，表示被审计对象的类型
     * @param targetId 目标ID，表示被审计对象的唯一标识
     * @param submitterId 提交者ID，表示提交审计请求的用户ID
     */
    private void createAuditLog(String targetType, Integer targetId, Integer submitterId) {
        // 创建一个新的审计日志对象
        AuditLog audit = new AuditLog();
        // 设置审计日志的目标类型
        audit.setTargetType(targetType);
        // 设置审计日志的目标ID
        audit.setTargetId(targetId);
        // 设置审计结果为"待处理"状态
        audit.setResult("PENDING");
        // 设置审计时间为当前系统时间
        audit.setAuditTime(LocalDateTime.now());
        // 调用审计日志服务保存审计日志
        auditLogService.save(audit);
    }

}
