package com.reggie.reg.service.impl;

import com.reggie.reg.entity.Notification;
import com.reggie.reg.mapper.NotificationMapper;
import com.reggie.reg.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 通知公告表 服务实现类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

}
