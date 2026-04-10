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
 * 问答互动表
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_qa")
public class CourseQa implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "qa_id", type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Integer qaId;

    private Integer courseId;

    private Integer studentId;

    /**
     * 回答教师 ID
     */
    private Integer teacherId;

    private String question;

    /**
     * 回答内容
     */
    private String answer;

    /**
     * 是否匿名提问
     */
    private Boolean isAnonymous;

    private LocalDateTime askTime;

    /**
     * 回答时间
     */
    private LocalDateTime answerTime;

    private String auditStatus;  // 添加这个字段

}
