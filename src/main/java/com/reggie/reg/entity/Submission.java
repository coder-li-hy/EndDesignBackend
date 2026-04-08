package com.reggie.reg.entity;

import java.math.BigDecimal;
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
 * 作业提交表
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("submission")
public class Submission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "submission_id", type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Integer submissionId;

    private Integer assignmentId;

    private Integer studentId;

    private String contentType;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文本内容
     */
    private String textContent;

    private LocalDateTime submitTime;

    /**
     * 是否迟交
     */
    private Boolean isLate;

    /**
     * 迟交理由
     */
    private String lateReason;

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 教师评语
     */
    private String teacherComment;

    /**
     * 批改时间
     */
    private LocalDateTime gradeTime;


}
