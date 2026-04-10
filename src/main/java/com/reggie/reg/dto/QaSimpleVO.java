package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问答互动 - 简化视图对象（用于审核列表展示）
 */
@Data
public class QaSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 基础信息
    private Integer qaId;
    private String question;        // 问题内容（核心展示字段）
    private String answer;          // 回答内容（如果有）
    private Boolean isAnonymous;    // 是否匿名（前端显示"匿名"标签用）

    // 关联信息
    private String courseName;      // 所属课程名称

    // 时间信息
    private LocalDateTime askTime;  // 提问时间
    private LocalDateTime answerTime; // 回答时间

    // 不需要返回的字段：
    // ❌ courseId, studentId, teacherId（用 submitter/auditor 代替）
}