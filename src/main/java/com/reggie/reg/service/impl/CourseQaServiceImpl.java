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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean submitQa(CourseQa qa) {
        // 1. 设置初始状态
        qa.setAuditStatus("PENDING");
        qa.setAskTime(LocalDateTime.now());

        // 2. 保存问答
        boolean saved = this.save(qa);
        if (!saved) return false;

        // 3. ⭐ 创建审核记录
        createAuditLog("QA", qa.getQaId(), qa.getStudentId());

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
