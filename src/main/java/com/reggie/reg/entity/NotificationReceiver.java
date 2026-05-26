package com.reggie.reg.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author lihy
 * @since 2026-05-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notification_receiver")
public class NotificationReceiver implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /*
     * 通知ID
     */
    private Integer notificationId;

    /*
     * 接收人ID
     */
    private Integer receiverId;

    /**
     * 是否已读 已读为1 未读为0 默认为0
     */
    private Boolean isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;


}
