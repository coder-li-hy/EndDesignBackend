// 2. AuditLogVO.java - 审核列表项（简化版，用于列表展示）
package com.reggie.reg.vo;
import com.reggie.reg.entity.*;
import lombok.Data;
import java.io.Serializable;

@Data
public class AuditLogVO implements Serializable {
    // 审核记录基础字段
    private Integer auditId;
    // 审核记录类型
    private String targetType;      // RESOURCE/QA/SUBMISSION
    // 审核目标ID
    private Integer targetId;
    // 审核结果
    private Integer auditorId;
    // 审核结果
    private String result;          // PENDING/PASS/REJECT
    // 审核拒绝理由
    private String reason;
    // 审核时间
    private String auditTime;

    // 关联内容摘要（三选一）
    // 资源视图VO
    private ResourceSimpleVO resource;
    // 问答视图VO
    private QaSimpleVO qa;
    // 提交视图VO
    private SubmissionSimpleVO submission;

    // 关联用户信息
    private UserSimpleVO submitter;
    // 关联管理员信息
    private UserSimpleVO auditor;

    // 辅助字段
    private String targetTypeText;
    private String resultText;
    private String contentPreview;
    private String submitTime;
}
