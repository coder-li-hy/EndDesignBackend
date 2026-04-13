package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.CourseResource;
import com.reggie.reg.entity.ResourceProgress;
import com.reggie.reg.entity.SysUser;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.ICourseResourceService;
import com.reggie.reg.service.IResourceProgressService;
import com.reggie.reg.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
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
 * 资源学习进度表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
public class ResourceProgressController {
    private final IResourceProgressService progressService;
    private final ICourseResourceService resourceService;
    private final ICourseInfoService courseService;
    private final ISysUserService userService;

//    /**
//     * 获取课程资源学习进度列表
//     * GET /api/teacher/resources/progress?courseId=1&studentName=&isCompleted=
//     */
//    @GetMapping("/teacher/resources/progress")
//    public R<Map<String, Object>> getProgressList(
//            @RequestParam Integer courseId,
//            @RequestParam(required = false) String studentName,
//            @RequestParam(required = false) Boolean isCompleted,
//            HttpServletRequest request) {
//
//        try {
//            // 1. 权限校验
//            Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
//            if (teacherId == null) {
//                return R.error("未登录");
//            }
//
//            // 2. 校验课程归属
//            CourseInfo course = courseService.getById(courseId);
//            if (course == null || !teacherId.equals(course.getTeacherId())) {
//                return R.error("无权访问");
//            }
//
//            // 3. 查本课程资源 ID 列表
//            List<Integer> resourceIds = resourceService.list(
//                    new LambdaQueryWrapper<CourseResource>()
//                            .select(CourseResource::getResourceId)
//                            .eq(CourseResource::getCourseId, courseId)
//            ).stream().map(CourseResource::getResourceId).collect(Collectors.toList());
//
//            if (resourceIds.isEmpty()) {
//                return R.success(buildEmptyResult());
//            }
//
//            // 4. 构建进度查询
//            LambdaQueryWrapper<ResourceProgress> query = new LambdaQueryWrapper<>();
//            query.in(ResourceProgress::getProgressId, resourceIds);
//
//            // 按学生姓名过滤
//            if (StringUtils.isNotBlank(studentName)) {
//                List<Integer> studentIds = userService.list(
//                        new LambdaQueryWrapper<SysUser>()
//                                .select(SysUser::getUserId)
//                                .like(SysUser::getUsername, studentName)
//                                .eq(SysUser::getRole, "STUDENT")
//                ).stream().map(SysUser::getUserId).collect(Collectors.toList());
//
//                if (studentIds.isEmpty()) {
//                    return R.success(buildEmptyResult());
//                }
//                query.in(ResourceProgress::getStudentId, studentIds);
//            }
//
//            // 按完成状态过滤
//            if (isCompleted != null) {
//                query.eq(ResourceProgress::getIsCompleted, isCompleted);
//            }
//
//            query.orderByDesc(ResourceProgress::getViewTime);
//
//            // 5. 查询进度记录
//            List<ResourceProgress> progressList = progressService.list(query);
//
//            // 6. 预加载关联数据（避免 N+1）
//            Map<Integer, CourseResource> resourceMap = resourceService.listByIds(resourceIds)
//                    .stream().collect(Collectors.toMap(CourseResource::getResourceId, r -> r));
//
//            List<Integer> studentIds = progressList.stream()
//                    .map(ResourceProgress::getStudentId)
//                    .distinct().collect(Collectors.toList());
//            Map<Integer, SysUser> studentMap = userService.listByIds(studentIds)
//                    .stream().collect(Collectors.toMap(SysUser::getUserId, u -> u));
//
//            // 7. 组装结果
//            List<Map<String, Object>> resultList = new ArrayList<>();
//            for (ResourceProgress p : progressList) {
//                Map<String, Object> item = new HashMap<>();
//                item.put("isCompleted", p.getIsCompleted());
//                item.put("viewTime", p.getViewTime());
//
//                CourseResource res = resourceMap.get(p.getResourceId());
//                if (res != null) {
//                    item.put("resourceTitle", res.getTitle());
//                    item.put("resourceType", res.getType());
//                }
//
//                SysUser stu = studentMap.get(p.getStudentId());
//                if (stu != null) {
//                    item.put("studentName", stu.getUsername());
//                }
//
//                resultList.add(item);
//            }
//
//            // 8. 返回
//            Map<String, Object> result = new HashMap<>();
//            result.put("list", resultList);
//            result.put("total", resultList.size());
//            return R.success(result);
//
//        } catch (Exception e) {
//            // 容错：异常时返回空列表，避免前端报错
//            System.err.println("Progress query error: " + e.getMessage());
//            return R.success(buildEmptyResult());
//        }
//    }

    // 辅助方法：构建空结果
    private Map<String, Object> buildEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("list", new ArrayList<>());
        result.put("total", 0);
        return result;
    }
}
