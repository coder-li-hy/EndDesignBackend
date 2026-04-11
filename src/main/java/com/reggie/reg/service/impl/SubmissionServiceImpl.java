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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean submitAssignment(Submission submission) {
        // 1. 设置初始状态
        submission.setAuditStatus("PENDING");
        submission.setSubmitTime(LocalDateTime.now());

        // 查询对应的Assignment的deadline
        LocalDateTime deadline = assignmentService.getById(submission.getAssignmentId()).getDeadline();

        // 2. 判断是否迟交（业务逻辑）
        if (submission.getSubmitTime().isAfter(deadline)) {
            submission.setIsLate(true);
        }

        // 3. 保存提交
        boolean saved = this.save(submission);
        if (!saved) return false;

        // 4. ⭐ 创建审核记录
        createAuditLog("SUBMISSION", submission.getSubmissionId(), submission.getStudentId());

        return true;
    }

    private void createAuditLog(String targetType, Integer targetId, Integer submitterId) {
        AuditLog audit = new AuditLog();
        audit.setTargetType(targetType);
        audit.setTargetId(targetId);
        audit.setResult("PENDING");
        audit.setAuditTime(LocalDateTime.now());
        auditLogService.save(audit);
    }

}
