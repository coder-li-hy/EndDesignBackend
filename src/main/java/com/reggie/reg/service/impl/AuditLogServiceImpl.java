package com.reggie.reg.service.impl;

import com.reggie.reg.entity.AuditLog;
import com.reggie.reg.mapper.AuditLogMapper;
import com.reggie.reg.service.IAuditLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 审核日志表 服务实现类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements IAuditLogService {

}
