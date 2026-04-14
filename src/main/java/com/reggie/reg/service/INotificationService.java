package com.reggie.reg.service;

import com.reggie.reg.common.R;
import com.reggie.reg.entity.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 通知公告表 服务类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
public interface INotificationService extends IService<Notification> {
    public R<String> sendSystemNotification(Notification notification, Integer adminId);

}
