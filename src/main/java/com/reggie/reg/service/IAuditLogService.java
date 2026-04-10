package com.reggie.reg.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.dto.AuditLogDetailVO;
import com.reggie.reg.dto.AuditLogVO;
import com.reggie.reg.dto.AuditQueryDTO;
import com.reggie.reg.dto.AuditStatsVO;
import com.reggie.reg.entity.AuditLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 审核日志表 服务类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
public interface IAuditLogService extends IService<AuditLog> {
    // ========== 查询接口 ==========

    /**
     * 获取审核统计
     */
    AuditStatsVO getAuditStats();

    /**
     * 分页查询审核列表（返回 VO）
     */
    Page<AuditLogVO> pageAuditVO(Page<AuditLog> page, AuditQueryDTO query);

    /**
     * 获取审核详情（完整信息）
     */
    AuditLogDetailVO getDetail(Integer auditId);

    // ========== 审核操作接口 ==========

    /**
     * 通过审核
     * @param auditId 审核记录 ID
     * @param adminId 操作管理员 ID
     */
    void approve(Integer auditId, Integer adminId);

    /**
     * 拒绝审核
     * @param auditId 审核记录 ID
     * @param adminId 操作管理员 ID
     * @param reason 拒绝原因
     */
    void reject(Integer auditId, Integer adminId, String reason);

    /**
     * 批量通过
     * @param auditIds 审核记录 ID 列表
     * @param adminId 操作管理员 ID
     */
    void batchApprove(List<Integer> auditIds, Integer adminId);

    /**
     * 批量拒绝
     * @param auditIds 审核记录 ID 列表
     * @param adminId 操作管理员 ID
     * @param reason 拒绝原因
     */
    void batchReject(List<Integer> auditIds, Integer adminId, String reason);

    /**
     * 重置为待审核（重审）
     */
    void resetToPending(Integer auditId);

    // ========== 内部辅助方法（protected，供子类或同包使用）==========

    /**
     * AuditLog 转 AuditLogVO（列表用）
     */
    AuditLogVO convertToVO(AuditLog log);

    /**
     * AuditLog 转 AuditLogDetailVO（详情用）
     */
    AuditLogDetailVO convertToDetailVO(AuditLog log);

}
