package com.reggie.reg.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 审核日志表
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "audit_id", type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Integer auditId;

    private String targetType;

    /**
     * 对应资源/问答/提交的 ID
     */
    private Integer targetId;

    /**
     * 审核管理员 ID
     */
    private Integer auditorId;

    private String result;

    /**
     * 违规原因
     */
    private String reason;

    private LocalDateTime auditTime;


}
