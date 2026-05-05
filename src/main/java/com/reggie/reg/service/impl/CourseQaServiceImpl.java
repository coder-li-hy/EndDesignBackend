package com.reggie.reg.service.impl;

import com.reggie.reg.entity.AuditLog;
import com.reggie.reg.entity.CourseQa;
import com.reggie.reg.mapper.CourseQaMapper;
import com.reggie.reg.service.IAuditLogService;
import com.reggie.reg.service.ICourseQaService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 问答互动表 服务实现类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Service
@RequiredArgsConstructor
public class CourseQaServiceImpl extends ServiceImpl<CourseQaMapper, CourseQa> implements ICourseQaService {
    private final IAuditLogService auditLogService;

    /**
     * 提交课程问答
     * @param qa 课程问答对象
     * @return 提交成功返回true，失败返回false
     * @Transactional 确保方法执行过程中出现异常时回滚，包括所有异常类型
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean submitQa(CourseQa qa) {
        // 1. 设置初始状态
        qa.setAuditStatus("PENDING");  // 设置审核状态为"待审核"
        qa.setAskTime(LocalDateTime.now());  // 设置提问时间为当前时间

        // 2. 保存问答
        boolean saved = this.save(qa);  // 调用保存方法，保存问答信息
        if (!saved) return false;  // 如果保存失败，直接返回false

        // 3. ⭐ 创建审核记录
        createAuditLog("QA", qa.getQaId(), qa.getStudentId());  // 创建审核日志记录，记录类型为"QA"，问答ID和学生ID

        return true;  // 返回true表示提交成功
    }

    /**
     * 创建审计日志
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param submitterId 提交者ID
     */
    private void createAuditLog(String targetType, Integer targetId, Integer submitterId) {
        // 创建新的审计日志对象
        AuditLog audit = new AuditLog();
        // 设置目标类型
        audit.setTargetType(targetType);
        // 设置目标ID
        audit.setTargetId(targetId);
        // 设置审核结果为"待处理"
        audit.setResult("PENDING");
        // 设置审核时间为当前时间
        audit.setAuditTime(LocalDateTime.now());
        // 保存审计日志
        auditLogService.save(audit);
    }

}
