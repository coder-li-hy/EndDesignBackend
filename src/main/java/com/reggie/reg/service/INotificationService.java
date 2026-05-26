package com.reggie.reg.service;

import com.reggie.reg.common.R;
import com.reggie.reg.entity.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 通知公告表 服务类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
public interface INotificationService extends IService<Notification> {
//    public R<String> sendSystemNotification(Notification notification, Integer adminId);

    // 📋 获取我的通知列表（分页 + 筛选）
    Map<String, Object> getMyNotifications(Integer userId, Integer courseId, String type, Boolean isRead, Integer page, Integer size);

    // 🔔 获取未读数量
    Integer getUnreadCount(Integer userId);

    // ✅ 标记通知为已读
    boolean markAsRead(Integer userId, Integer notifyId);

    //  教师发布课程通知
    boolean publishNotification(Notification notification);


    public boolean publishAdminNotification(Notification notification);

}
