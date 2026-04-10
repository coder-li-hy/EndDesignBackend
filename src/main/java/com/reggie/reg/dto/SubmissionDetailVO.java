package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交 - 完整详情视图对象
 */
@Data
public class SubmissionDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 基础字段 ==========
    private Integer submissionId;
    private String contentType;       // FILE/TEXT/CODE
    private String filePath;          // 文件路径
    private String textContent;       // 文本内容（完整）
    private Boolean isLate;           // 是否迟交
    private String lateReason;        // 迟交理由

    // 评分信息
    private BigDecimal score;
    private String teacherComment;    // 教师评语（完整）

    // ========== 扩展字段 ==========

    /**
     * 所属作业完整信息
     */
    private AssignmentDetailVO assignment;

    /**
     * 提交学生完整信息
     */
    private UserDetailVO student;

    /**
     * 批改教师完整信息（如果已批改）
     */
    private UserDetailVO grader;

    /**
     * 代码提交特有字段（如果 contentType=CODE）
     */
    private String codeLanguage;      // 示例：java, python, c++
    private String codePreview;       // 代码前 500 字符预览
    private Integer codeLineCount;    // 代码行数

    /**
     * 文件提交特有字段（如果 contentType=FILE）
     */
    private String originalFileName;  // 原始文件名
    private Long fileSize;            // 文件大小
    private String fileHash;          // 文件哈希（防篡改）

    /**
     * 提交状态
     */
    private String submissionStatus;  // SUBMITTED/GRADING/COMPLETED

    /**
     * 批改详情（如果已批改）
     */
    private GradeDetailVO gradeDetail;

    // 时间信息
    private LocalDateTime submitTime;
    private LocalDateTime gradeTime;
    private LocalDateTime updateTime;
}