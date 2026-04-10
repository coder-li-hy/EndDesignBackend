package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审核操作记录 - 视图对象
 * 用于展示审核过程的完整历史
 */
@Data
public class AuditOperationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer operationId;
    private Integer auditId;            // 关联的审核记录 ID

    // 操作信息
    private String operationType;       // SUBMIT/APPROVE/REJECT/REAUDIT
    private String operationText;       // 中文操作类型

    // 操作结果
    private String result;              // PENDING/PASS/REJECT
    private String resultText;          // 中文结果
    private String reason;              // 拒绝原因（如果有）

    // 操作人
    private UserSimpleVO operator;      // 操作的管理员

    // 时间
    private LocalDateTime operateTime;

    // 备注
    private String remark;              // 操作备注
}