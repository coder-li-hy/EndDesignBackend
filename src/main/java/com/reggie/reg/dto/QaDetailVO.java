package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问答互动 - 完整详情视图对象
 */
@Data
public class QaDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 基础字段 ==========
    private Integer qaId;
    private String question;          // 问题内容（完整，不截断）
    private String answer;            // 回答内容（如果有）
    private Boolean isAnonymous;      // 是否匿名

    // ========== 扩展字段 ==========

    /**
     * 所属课程完整信息
     */
    private CourseSimpleVO course;

    /**
     * 提问学生完整信息（如果非匿名）
     */
    private UserDetailVO student;

    /**
     * 回答教师完整信息（如果有回答）
     */
    private UserDetailVO teacher;

    /**
     * 问题标签/分类（如果有）
     */
    private String tags;              // 示例："期末考,范围,第 10 章"

    /**
     * 问题状态
     */
    private String qaStatus;          // OPEN/ANSWERED/CLOSED

    /**
     * 被采纳标记
     */
    private Boolean isAccepted;       // 是否被标记为"最佳答案"

    /**
     * 点赞/回复数（互动数据）
     */
    private Integer likeCount;
    private Integer replyCount;

    // 时间信息
    private LocalDateTime askTime;
    private LocalDateTime answerTime;
    private LocalDateTime updateTime;
}