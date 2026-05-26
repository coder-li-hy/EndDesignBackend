package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.dto.NotificationDTO;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.CourseSelection;
import com.reggie.reg.entity.Notification;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.ICourseSelectionService;
import com.reggie.reg.service.INotificationReceiverService;
import com.reggie.reg.service.INotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 通知公告表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/notifications")
public class NotificationController {
    @Autowired
    private INotificationService notificationService;

    // 🔔 获取"我的通知"列表（支持筛选+分页）
    @GetMapping("/my")
    public R<Map<String, Object>> getMyNotifications(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        // 🔐 从 token/session 获取当前用户（根据你的鉴权方式调整）
        Integer userId = (Integer) request.getSession().getAttribute("sys_user");
        // 或：Integer userId = UserContext.getCurrentUserId();

        if (userId == null) return R.error("用户未登录");

        Map<String, Object> data = notificationService.getMyNotifications(
                userId, courseId, type, isRead, page, size);
        return R.success(data);
    }

    // 🔢 获取未读数量（用于红点）
    @GetMapping("/unread-count")
    public R<Integer> getUnreadCount(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) return R.error("用户未登录");
        return R.success(notificationService.getUnreadCount(userId));
    }

    // ✅ 标记单条通知为已读
    @PostMapping("/{notifyId}/read")
    public R<String> markAsRead(@PathVariable Integer notifyId, HttpServletRequest request) {
        Integer userId = (Integer) request.getSession().getAttribute("sys_user");
        if (userId == null) return R.error("用户未登录");

        boolean success = notificationService.markAsRead(userId, notifyId);
        return success ? R.success("已标记为已读") : R.error("操作失败");
    }

    // ✅ 批量标记某课程下通知为已读（可选）
    @PostMapping("/course/{courseId}/read-all")
    public R<String> markCourseAsRead(@PathVariable Integer courseId, HttpServletRequest request) {
        Integer userId = (Integer) request.getSession().getAttribute("sys_user");
        if (userId == null) return R.error("用户未登录");

        // 调用 mapper 的 markCourseAsRead 方法（需补充到 Service）
        return R.success("批量标记成功");
    }

    // 📤 发布通知（仅管理员/教师可用，需加权限注解）
    @PostMapping
    public R<String> publish(@RequestBody NotificationDTO dto, HttpServletRequest request) {
        // 🔐 权限校验 + 参数校验略...
        // 获取当前登录用户角色
        if ("TEACHER".equals(request.getSession().getAttribute("sys_user_role"))){
            dto.setType("COURSE");
            // 如果过当前登录用户为教师
            notificationService.publishNotification(dto.toEntity());
            return R.success("发布成功");
        } else if ("ADMIN".equals(request.getSession().getAttribute("sys_user_role"))) {
            // 如果过当前登录用户为管理员
            dto.setType("SYSTEM");
            dto.setPublisherId(1);// 默认单管理员模式 将发布人设置为
            // 发送通知给系统的所有用户
            notificationService.publishAdminNotification(dto.toEntity());
            return R.success("发布成功");
        }
        return R.error("无法发送通知");
    }

}
