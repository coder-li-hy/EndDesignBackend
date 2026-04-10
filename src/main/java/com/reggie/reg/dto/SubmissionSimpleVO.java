package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交 - 简化视图对象（用于审核列表展示）
 */
@Data
public class SubmissionSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 基础信息
    private Integer submissionId;
    private String assignmentTitle;  // 作业标题（需要关联查询 Assignment 表）
    private String contentType;      // 提交类型：FILE/TEXT/CODE

    // 提交内容（根据类型返回不同字段）
    private String filePath;         // 文件路径（contentType=FILE 时）
    private String textContent;      // 文本内容（contentType=TEXT 时，前端可截断显示）

    // 提交状态
    private Boolean isLate;          // 是否迟交（前端显示标签用）
    private String lateReason;       // 迟交理由（如果有）

    // 评分信息（如果已批改）
    private BigDecimal score;
    private String teacherComment;

    // 时间信息
    private LocalDateTime submitTime;   // 提交时间
    private LocalDateTime gradeTime;    // 批改时间

    // 不需要返回的字段：
    // ❌ assignmentId, studentId（用其他对象代替）
}