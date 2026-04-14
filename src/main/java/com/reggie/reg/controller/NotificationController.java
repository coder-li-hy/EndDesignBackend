package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.CourseSelection;
import com.reggie.reg.entity.Notification;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.ICourseSelectionService;
import com.reggie.reg.service.INotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class NotificationController {
    private final INotificationService notificationService;
    private final ICourseInfoService courseService;
    private final ICourseSelectionService courseSelectionService;

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

    /**
     * 1. 发送课程通知（为每个选课学生创建独立通知记录）
     * POST /api/teacher/notifications/send
     *
     * 逻辑：教师给课程 1 发通知 → 查课程 1 的所有已选学生 →
     *      为每个学生创建一条 notification 记录（receiverId=学生 ID）
     */
    @PostMapping("/teacher/notifications/send")
    public R<String> sendCourseNotification(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {

        try {
            // 1. 参数校验
            Integer courseId = (Integer) params.get("courseId");
            Integer teacherId = (Integer) params.get("teacherId");
            String title = (String) params.get("title");
            String content = (String) params.get("content");

            if (courseId == null || teacherId == null ||
                    title == null || title.trim().isEmpty() ||
                    content == null || content.trim().isEmpty()) {
                return R.error("参数错误");
            }

            // 权限校验
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(teacherId)) {
                return R.error("无权操作");
            }

            // 2. 校验课程归属
            CourseInfo course = courseService.getById(courseId);
            if (course == null) {
                return R.error("课程不存在");
            }
            if (!teacherId.equals(course.getTeacherId())) {
                return R.error("无权给该课程发送通知");
            }

            // ⭐ 3. 查询该课程的所有已选学生
            List<CourseSelection> selections = courseSelectionService.list(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)
                            .eq(CourseSelection::getStatus, "SELECTED")  // 只查已选（不含排队）
            );

            if (selections.isEmpty()) {
                return R.error("该课程暂无已选学生，无法发送通知");
            }

            // ⭐ 4. 为每个学生创建独立通知记录
            List<Notification> notifications = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (CourseSelection selection : selections) {
                Notification notification = new Notification();
                notification.setPublisherId(teacherId);      // 发布者：当前教师
                notification.setCourseId(courseId);          // 关联课程
                notification.setType("COURSE");              // 类型：课程通知
                notification.setTitle(title.trim());
                notification.setContent(content.trim());
                notification.setPublishTime(now);            // 同一批通知时间相同
                notification.setReceiverId(selection.getStudentId());  // ⭐ 接收者：具体学生

                notifications.add(notification);
            }

            // ⭐ 批量保存（比循环 save 更高效）
            if (!notifications.isEmpty()) {
                notificationService.saveBatch(notifications);
            }

            return R.success("通知发送成功，共 " + notifications.size() + " 名学生收到");

        } catch (Exception e) {
            System.err.println("Send notification error: " + e.getMessage());
            return R.error("发送失败");
        }
    }

    /**
     * 管理员发送系统通知
     * POST /admin/notification/sendSystem
     */
    @PostMapping("/admin/notification/sendSystem")
    public R<String> sendSystemNotification(@RequestBody Notification notification,
                                            HttpServletRequest request) {
        try {
            // 从session或token中获取管理员ID（根据实际鉴权方式调整）
            Integer adminId = (Integer) request.getSession().getAttribute("adminId");
            if (adminId == null) {
                adminId = 1; // 降级处理：默认系统管理员
            }
            log.info("管理员[{}]发送系统通知: {}", adminId, notification.getTitle());
            return notificationService.sendSystemNotification(notification, adminId);
        } catch (Exception e) {
            log.error("发送系统通知异常", e);
            return R.error("系统异常，发送失败");
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
