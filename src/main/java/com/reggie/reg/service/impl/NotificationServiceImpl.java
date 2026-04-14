package com.reggie.reg.service.impl;

import com.reggie.reg.common.R;
import com.reggie.reg.entity.Notification;
import com.reggie.reg.mapper.NotificationMapper;
import com.reggie.reg.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    /**
     * 管理员发送系统通知
     * @param notification 前端传入的通知对象（仅需title/content）
     * @param adminId 从令牌解析的管理员ID
     * @return 通用返回结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> sendSystemNotification(Notification notification, Integer adminId) {
        // 1. 参数校验
        if (notification == null) {
            return R.error("请求参数不能为空");
        }
        String title = notification.getTitle();
        String content = notification.getContent();
        if (title == null || title.trim().isEmpty()) {
            return R.error("通知标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return R.error("通知内容不能为空");
        }

        // 2. 封装系统通知固定字段（严格遵循业务规则）
        notification.setPublisherId(adminId != null ? adminId : 1);  // 管理员ID，默认1
        notification.setCourseId(null);              // ⚠️ 与课程无关设为0
        notification.setType("SYSTEM");           // ⚠️ 系统通知类型
        notification.setReceiverId(0);            // ⚠️ 接收者为所有人
        notification.setPublishTime(LocalDateTime.now());

        // 3. 持久化保存
        boolean saved = this.save(notification);
        if (saved) {
            return R.success("系统通知发送成功");
        }
        return R.error("系统通知发送失败，请稍后重试");
    }

}
