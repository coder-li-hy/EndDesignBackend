// src/main/java/com/reggie/reg/mapper/NotificationMapper.java
package com.reggie.reg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.reg.entity.Notification;
import com.reggie.reg.entity.NotificationReceiver;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface NotificationMapper extends BaseMapper<Notification> {

    @Transactional
    //  核心：查询"我的通知"列表（含已读状态）
    List<Map<String, Object>> selectMyNotifications(
            @Param("userId") Integer userId,
            @Param("courseId") Integer courseId,  // 可选：按课程筛选
            @Param("type") String type,           // 可选：按类型筛选
            @Param("isRead") Boolean isRead,      // 可选：只看未读
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    @Transactional
    //  统计未读数量（用于红点）
    Integer countUnread(@Param("userId") Integer userId);

    @Transactional
    //  标记单条通知为已读
    int markAsRead(@Param("userId") Integer userId, @Param("notifyId") Integer notifyId);

    @Transactional
    //  标记某课程下所有通知为已读（批量）
    int markCourseAsRead(@Param("userId") Integer userId, @Param("courseId") Integer courseId);
}