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
     * 保存课程资源信息
     * @param resource 课程资源对象，包含资源的各项信息
     * @return 保存成功返回true，失败返回false
     * @Transactional 确保方法执行过程中出现异常时进行事务回滚
     * @rollbackFor = Exception.class 指定所有异常都触发回滚
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveResource(CourseResource resource) {
        // 1. 设置初始状态 - 初始化资源的审核状态和创建时间
        resource.setAuditStatus("PENDING");  // 业务表状态：设置为待审核状态
        resource.setCreateTime(LocalDateTime.now()); // 设置资源创建时间为当前时间

        // 2. 保存资源 - 将资源信息持久化到数据库
        boolean saved = this.save(resource);
        if (!saved) {
            return false; // 保存失败则返回false
        }

        // 3. 自动创建审核记录 - 资源保存成功后，自动创建一条审核记录
        createAuditLog("RESOURCE", resource.getResourceId(), resource.getUploaderId());

        return true; // 资源保存成功并创建审核记录后返回true
    }


    /**
     * 创建审核日志记录
     * @param targetType 目标类型，可以是 RESOURCE/QA/SUBMISSION
     * @param targetId 目标ID，对应资源/问答/作业的ID
     * @param submitterId 提交者ID
     */
    private void createAuditLog(String targetType, Integer targetId, Integer submitterId) {
        AuditLog audit = new AuditLog();    // 创建新的审核日志对象
        audit.setTargetType(targetType);        // RESOURCE/QA/SUBMISSION
        audit.setTargetId(targetId);            // 资源/问答/作业的 ID
        audit.setResult("PENDING");             // 初始状态：待审核
        audit.setAuditTime(LocalDateTime.now());
        // auditorId 和 reason 留空，等管理员审核时再填
        auditLogService.save(audit);
    }
}
