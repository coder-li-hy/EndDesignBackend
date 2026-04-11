package com.reggie.reg.service.impl;

import com.reggie.reg.entity.AuditLog;
import com.reggie.reg.entity.CourseResource;
import com.reggie.reg.mapper.CourseResourceMapper;
import com.reggie.reg.service.IAuditLogService;
import com.reggie.reg.service.ICourseResourceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 课程资源表 服务实现类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Service
@RequiredArgsConstructor
public class CourseResourceServiceImpl extends ServiceImpl<CourseResourceMapper, CourseResource> implements ICourseResourceService {

    private final IAuditLogService auditLogService;  // ⭐ 注入审核服务

    /**
     * 上传课程资源（自动创建审核记录）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveResource(CourseResource resource) {
        // 1. 设置初始状态
        resource.setAuditStatus("PENDING");  // ⭐ 业务表状态
        resource.setCreateTime(LocalDateTime.now());

        // 2. 保存资源
        boolean saved = this.save(resource);
        if (!saved) {
            return false;
        }

        // 3. ⭐ 自动创建审核记录
        createAuditLog("RESOURCE", resource.getResourceId(), resource.getUploaderId());

        return true;
    }

    /**
     * 创建审核记录的通用方法
     */
    private void createAuditLog(String targetType, Integer targetId, Integer submitterId) {
        AuditLog audit = new AuditLog();
        audit.setTargetType(targetType);        // RESOURCE/QA/SUBMISSION
        audit.setTargetId(targetId);            // 资源/问答/作业的 ID
        audit.setResult("PENDING");             // 初始状态：待审核
        audit.setAuditTime(LocalDateTime.now());
        // auditorId 和 reason 留空，等管理员审核时再填
        auditLogService.save(audit);
    }
}
