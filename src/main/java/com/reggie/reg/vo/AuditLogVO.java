// 2. AuditLogVO.java - 审核列表项（简化版，用于列表展示）
package com.reggie.reg.vo;
import com.reggie.reg.entity.*;
import lombok.Data;
import java.io.Serializable;

@Data
public class AuditLogVO implements Serializable {
    // 审核记录基础字段
    private Integer auditId;
    private String targetType;      // RESOURCE/QA/SUBMISSION
    private Integer targetId;
    private Integer auditorId;
    private String result;          // PENDING/PASS/REJECT
    private String reason;
    private String auditTime;

    // 关联内容摘要（三选一）
    private ResourceSimpleVO resource;
    private QaSimpleVO qa;
    private SubmissionSimpleVO submission;

    // 关联用户信息
    private UserSimpleVO submitter;
    private UserSimpleVO auditor;

    // 辅助字段
    private String targetTypeText;
    private String resultText;
    private String contentPreview;
    private String submitTime;
}
