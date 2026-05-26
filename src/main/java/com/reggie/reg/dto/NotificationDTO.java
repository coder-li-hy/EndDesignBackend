// src/main/java/com/reggie/reg/dto/NotificationDTO.java
package com.reggie.reg.dto;

import com.reggie.reg.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 📤 发布通知的请求参数（DTO）
 * 用于接收前端提交的通知发布数据，与实体解耦
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class NotificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 🎯 通知基本信息
     */
    @NotNull(message = "课程ID不能为空")
    private Integer courseId;  // 0 表示系统级通知

    @NotBlank(message = "通知类型不能为空")
    private String type;       // 枚举：SYSTEM / COURSE

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private int publisherId;

    /**
     *  接收人列表（暂时不用）
     */
    private List<Integer> receiverIds;

    /**
     * 🔧 可选扩展字段
     */
    private Boolean needConfirm;      // 是否需要确认阅读（预留）
    private LocalDateTime expireTime; // 通知过期时间（预留）


    // 🔄 DTO → Entity 转换方法（保持实体纯净）
    public Notification toEntity() {
        Notification notification = new Notification();
        notification.setCourseId(this.courseId);
        notification.setType(this.type);
        notification.setTitle(this.title);
        notification.setContent(this.content);
        notification.setPublisherId(this.publisherId);

        return notification;
    }

    // 🎯 便捷构建器（可选）
    public static NotificationDTO ofSystem(String title, String content, List<Integer> receiverIds) {
        return new NotificationDTO()
                .setCourseId(0)
                .setType("SYSTEM")
                .setTitle(title)
                .setContent(content)
                .setReceiverIds(receiverIds);
    }

    public static NotificationDTO ofCourse(Integer courseId, String title, String content, List<Integer> receiverIds) {
        return new NotificationDTO()
                .setCourseId(courseId)
                .setType("COURSE")
                .setTitle(title)
                .setContent(content)
                .setReceiverIds(receiverIds);
    }
}