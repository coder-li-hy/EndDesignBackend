package com.reggie.reg.dto;

import com.reggie.reg.entity.AuditLog;
import lombok.Data;

import java.time.LocalDateTime;

// AuditLogVO.java (列表展示用)
@Data
public class AuditLogVO extends AuditLog {
    // 关联的内容对象（只包含必要字段）
    private ResourceSimpleVO resource;
    private QaSimpleVO qa;
    private SubmissionSimpleVO submission;

    // 提交者信息
    private UserSimpleVO submitter;

    // 审核人信息
    private UserSimpleVO auditor;
    // ========== 辅助字段（方便前端）==========
    private String targetTypeText;   // 类型中文
    private String resultText;       // 结果中文
    private String contentPreview;   // 内容预览文本
    private LocalDateTime submitTime; // 提交时间（统一字段）
}