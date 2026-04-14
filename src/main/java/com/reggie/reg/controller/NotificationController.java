package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.Notification;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.INotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
public class NotificationController {
    private final INotificationService notificationService;
    private final ICourseInfoService courseService;

    /**
     * 1. 获取用户的通知列表（系统通知 + 个人通知）
     * GET /api/notifications/my?userId=100&role=STUDENT&page=1&size=10
     */
    @GetMapping("/notifications/my")
    public R<Map<String, Object>> getMyNotifications(
            @RequestParam Integer userId,
            @RequestParam String role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        try {
            // 1. 权限校验：只能查自己的
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(userId)) {
                return R.error("无权访问");
            }

            // 2. 构建查询条件
            LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<>();

            // 系统通知（发给所有人）+ 个人通知（发给当前用户）
            query.and(q -> q
                    .eq(Notification::getReceiverId, 0)  // 系统通知
                    .or()
                    .eq(Notification::getReceiverId, userId)  // 个人通知
            );

            query.orderByDesc(Notification::getPublishTime);

            // 3. 分页查询
            Page<Notification> notifyPage = notificationService.page(new Page<>(page, size), query);

            if (notifyPage.getRecords().isEmpty()) {
                return R.success(buildEmptyResult());
            }

            // 4. 预加载课程名称（仅课程通知需要）
            List<Integer> courseIds = notifyPage.getRecords().stream()
                    .filter(n -> n.getCourseId() != null && n.getCourseId() != 0)
                    .map(Notification::getCourseId)
                    .distinct().collect(Collectors.toList());

            Map<Integer, String> courseNameMap = new HashMap<>();
            if (!courseIds.isEmpty()) {
                courseService.listByIds(courseIds).forEach(c ->
                        courseNameMap.put(c.getCourseId(), c.getCourseName()));
            }

            // 5. 组装结果
            List<Map<String, Object>> resultList = notifyPage.getRecords().stream().map(n -> {
                Map<String, Object> item = new HashMap<>();
                item.put("notifyId", n.getNotifyId());
                item.put("type", n.getType());  // SYSTEM 或 COURSE
                item.put("title", n.getTitle());
                item.put("content", n.getContent());
                item.put("publishTime", n.getPublishTime());
                item.put("courseName", courseNameMap.get(n.getCourseId()));  // 课程通知显示课程名
                return item;
            }).collect(Collectors.toList());

            // 6. 返回
            Map<String, Object> result = new HashMap<>();
            result.put("list", resultList);
            result.put("total", notifyPage.getTotal());
            return R.success(result);

        } catch (Exception e) {
            System.err.println("Get notifications error: " + e.getMessage());
            return R.success(buildEmptyResult());  // 容错
        }
    }

    // 辅助方法：构建空结果
    private Map<String, Object> buildEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("list", new ArrayList<>());
        result.put("total", 0);
        return result;
    }

}
