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
 * 学习进度表
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("resource_progress")
public class ResourceProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "progress_id", type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Integer progressId;

    /**
     * 对应的作业Id
     */
    private Integer assignmentId;

    private Integer studentId;

    /**
     * 是否完成查看
     */
    private Boolean isCompleted;

    private LocalDateTime viewTime;


}
