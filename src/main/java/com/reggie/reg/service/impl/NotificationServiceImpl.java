package com.reggie.reg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.CourseSelection;
import com.reggie.reg.entity.Notification;
import com.reggie.reg.entity.NotificationReceiver;
import com.reggie.reg.entity.SysUser;
import com.reggie.reg.mapper.NotificationMapper;
import com.reggie.reg.service.ICourseSelectionService;
import com.reggie.reg.service.INotificationReceiverService;
import com.reggie.reg.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.reg.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private NotificationMapper notificationMapper;
    ;
    @Autowired
    private INotificationReceiverService receiverService;

    @Autowired
    private ICourseSelectionService courseSelectionService;

    @Autowired
    private ISysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getMyNotifications(Integer userId, Integer courseId,
                                                  String type, Boolean isRead,
                                                  Integer page, Integer size) {
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;

        List<Map<String, Object>> records = notificationMapper.selectMyNotifications(
                userId, courseId, type, isRead, (page - 1) * size, size);

        // 单独查总数（避免 COUNT(*) 在关联查询中性能问题）
        LambdaQueryWrapper<NotificationReceiver> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(NotificationReceiver::getReceiverId, userId);
        if (isRead != null) countWrapper.eq(NotificationReceiver::getIsRead, isRead);
        // 注意：courseId/type 筛选需关联主表，此处简化，实际可加 SQL 计数

        long total = receiverService.count(countWrapper); // 简化版，实际建议用 XML 计数

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer getUnreadCount(Integer userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Integer userId, Integer notifyId) {
        return notificationMapper.markAsRead(userId, notifyId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishNotification(Notification notification) {
        // 1️⃣ 插入通知主表
        notification.setPublishTime(LocalDateTime.now());
        this.save(notification);

        LambdaQueryWrapper<CourseSelection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseSelection::getCourseId, notification.getCourseId());
        List<CourseSelection> courseSelections = courseSelectionService.list(queryWrapper);
        // 获取选取该课程的所有学生id
        List<Integer> receiverIds = courseSelections.stream().
                map(CourseSelection::getStudentId).toList();

        // 2️⃣ 批量插入接收记录
        if (receiverIds != null && !receiverIds.isEmpty()) {
            List<NotificationReceiver> receivers = receiverIds.stream()
                    .map(uid -> new NotificationReceiver()
                            .setNotificationId(notification.getNotifyId())
                            .setReceiverId(uid)
                            .setIsRead(false))
                    .toList();
            receiverService.saveBatch(receivers);
        }
        return true;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishAdminNotification(Notification notification) {
        notification.setPublishTime(LocalDateTime.now());
        this.save(notification);
        // 获取所有用户id
        List<Integer> receiverIds = sysUserService.list().stream().
                map(SysUser::getUserId).toList();

        // 2️⃣ 批量插入接收记录
        if (receiverIds != null && !receiverIds.isEmpty()) {
            List<NotificationReceiver> receivers = receiverIds.stream()
                    .map(uid -> new NotificationReceiver()
                            .setNotificationId(notification.getNotifyId())
                            .setReceiverId(uid)
                            .setIsRead(false))
                    .toList();
            receiverService.saveBatch(receivers);
        }
        return true;
    }

}
