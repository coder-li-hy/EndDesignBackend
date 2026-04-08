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
 * 作业表
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("assignment")
public class Assignment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "assignment_id", type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Integer assignmentId;

    private Integer courseId;

    private String title;

    /**
     * 作业要求
     */
    private String description;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 是否允许迟交
     */
    private Boolean allowLate;


}
