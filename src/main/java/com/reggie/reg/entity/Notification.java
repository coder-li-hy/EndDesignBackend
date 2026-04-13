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
 * 通知公告表
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "notify_id", type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Integer notifyId;

    /**
     * 发布者ID 如果是系统通知则为1 本系统采用单管理员 只有管理员才有权力发送系统通知
     */
    private Integer publisherId;

    /**
     * 若与课程无关则为0 无需传入
     */
    private Integer courseId;

    private String type;

    private String title;

    private String content;

    private LocalDateTime publishTime;

    /**
     * 接收者ID 如果为系统通知 则为0 发给所有人
     */
    private Integer receiverId;


}
