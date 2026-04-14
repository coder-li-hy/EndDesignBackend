package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.AuditLog;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.CourseQa;
import com.reggie.reg.entity.CourseSelection;
import com.reggie.reg.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 问答互动表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
public class CourseQaController {
    private final ICourseQaService courseQaService;
    private final ICourseSelectionService courseSelectionService;
    private final IAuditLogService auditLogService;
    private final ICourseInfoService courseInfoService;
    private final ISysUserService sysUserService;

    /**
     * 1. 获取学生的课程提问列表
     * GET /api/student/qa?courseId=1&studentId=100
     */
    @GetMapping("/student/qa")
    public R<List<Map<String, Object>>> getStudentQaList(
            @RequestParam Integer courseId,
            @RequestParam Integer studentId,
            HttpServletRequest request) {

        try {
            // 1. 权限校验
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            // 校验课程是否已选
            CourseSelection selection = courseSelectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)
                            .eq(CourseSelection::getStudentId, studentId)
                            .eq(CourseSelection::getStatus, "SELECTED")
            );
            if (selection == null) {
                return R.error("请先选课");
            }

            // 2. 查询该学生的提问记录
            List<CourseQa> qaList = courseQaService.list(
                    new LambdaQueryWrapper<CourseQa>()
                            .eq(CourseQa::getCourseId, courseId)
                            .eq(CourseQa::getStudentId, studentId)
                            .orderByDesc(CourseQa::getAskTime)
            );

            if (qaList.isEmpty()) {
                return R.success(new ArrayList<>());
            }

            // 3. 组装结果（简化：只返回必要字段）
            List<Map<String, Object>> resultList = qaList.stream().map(qa -> {
                Map<String, Object> item = new HashMap<>();
                item.put("qaId", qa.getQaId());
                item.put("question", qa.getQuestion());
                item.put("isAnonymous", qa.getIsAnonymous());
                item.put("askTime", qa.getAskTime());
                item.put("auditStatus", qa.getAuditStatus());

                // ⭐ 仅审核通过后，才返回教师回复
                if ("PASS".equals(qa.getAuditStatus())) {
                    item.put("answer", qa.getAnswer());
                    item.put("answerTime", qa.getAnswerTime());
                    item.put("teacherId", qa.getTeacherId());  // 可选：用于显示教师姓名
                }

                return item;
            }).collect(Collectors.toList());

            return R.success(resultList);

        } catch (Exception e) {
            System.err.println("Get QA error: " + e.getMessage());
            return R.success(new ArrayList<>());  // 容错
        }
    }

    /**
     * 2. 发起提问
     * POST /api/student/qa
     */
    @PostMapping("/student/qa")
    public R<String> askQuestion(@RequestBody Map<String, Object> params, HttpServletRequest request) {

        try {
            // 1. 参数校验
            Integer courseId = (Integer) params.get("courseId");
            Integer studentId = (Integer) params.get("studentId");
            String question = (String) params.get("question");

            if (courseId == null || studentId == null || question == null || question.trim().isEmpty()) {
                return R.error("参数错误");
            }

            // 权限校验
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权操作");
            }

            // 2. 校验课程是否已选
            CourseSelection selection = courseSelectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)
                            .eq(CourseSelection::getStudentId, studentId)
                            .eq(CourseSelection::getStatus, "SELECTED")
            );
            if (selection == null) {
                return R.error("请先选课");
            }

            // 3. 创建提问记录
            CourseQa qa = new CourseQa();
            qa.setCourseId(courseId);
            qa.setStudentId(studentId);
            qa.setQuestion(question.trim());
            qa.setIsAnonymous(Boolean.TRUE.equals(params.get("isAnonymous")));
            qa.setAskTime(LocalDateTime.now());

            // ⭐ 审核状态：提交后进入待审核
            qa.setAuditStatus("PENDING");

            courseQaService.save(qa);

            // ⭐⭐ 创建审核日志记录
            createAuditLogForQa(qa.getQaId(), studentId);

            return R.success("提问成功，等待审核");

        } catch (Exception e) {
            System.err.println("Ask question error: " + e.getMessage());
            return R.error("提交失败");
        }
    }

    /**
     * 1. 获取教师课程的提问列表（仅审核通过的）
     * GET /api/teacher/qa?courseId=1&teacherId=200
     */
    @GetMapping("/teacher/qa")
    public R<List<Map<String, Object>>> getTeacherQaList(
            @RequestParam Integer courseId,
            @RequestParam Integer teacherId,
            HttpServletRequest request) {

        try {
            // 1. 权限校验
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(teacherId)) {
                return R.error("无权访问");
            }

            // 校验课程是否属于该教师 ?? 前端其实做了限制 但是后端可以保险一点
            CourseInfo course = courseInfoService.getById(courseId);
            if (course == null || !teacherId.equals(course.getTeacherId())) {
                return R.error("无权访问该课程");
            }

            // 2. 查询该课程下审核通过的提问
            List<CourseQa> qaList = courseQaService.list(
                    new LambdaQueryWrapper<CourseQa>()
                            .eq(CourseQa::getCourseId, courseId)
                            .eq(CourseQa::getAuditStatus, "PASS")  // ⭐ 只显示审核通过的
                            .orderByDesc(CourseQa::getAskTime)
            );

            if (qaList.isEmpty()) {
                return R.success(new ArrayList<>());
            }

            // 3. 预加载学生姓名（匿名提问不显示）
            List<Integer> studentIds = qaList.stream()
                    .filter(qa -> !Boolean.TRUE.equals(qa.getIsAnonymous()))
                    .map(CourseQa::getStudentId)
                    .distinct().collect(Collectors.toList());

            Map<Integer, String> studentNameMap = new HashMap<>();
            if (!studentIds.isEmpty()) {
                sysUserService.listByIds(studentIds).forEach(u ->
                        studentNameMap.put(u.getUserId(), u.getUsername()));
            }

            // 4. 组装结果
            List<Map<String, Object>> resultList = qaList.stream().map(qa -> {
                Map<String, Object> item = new HashMap<>();
                item.put("qaId", qa.getQaId());
                item.put("question", qa.getQuestion());
                item.put("isAnonymous", qa.getIsAnonymous());
                item.put("askTime", qa.getAskTime());
                item.put("answer", qa.getAnswer());
                item.put("answerTime", qa.getAnswerTime());

                // 非匿名提问才显示学生姓名
                if (!Boolean.TRUE.equals(qa.getIsAnonymous())) {
                    item.put("studentName", studentNameMap.get(qa.getStudentId()));
                }

                return item;
            }).collect(Collectors.toList());

            return R.success(resultList);

        } catch (Exception e) {
            System.err.println("Get teacher QA error: " + e.getMessage());
            return R.success(new ArrayList<>());  // 容错
        }
    }

    /**
     * 2. 回复提问
     * PUT /api/teacher/qa/{qaId}/answer
     */
    @PutMapping("/qa/{qaId}/answer")
    public R<String> replyQuestion(
            @PathVariable Integer qaId,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {

        try {
            // 1. 参数校验
            Integer teacherId = (Integer) params.get("teacherId");
            String answer = (String) params.get("answer");

            if (qaId == null || teacherId == null || answer == null || answer.trim().isEmpty()) {
                return R.error("参数错误");
            }

            // 权限校验
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(teacherId)) {
                return R.error("无权操作");
            }

            // 2. 查提问记录
            CourseQa qa = courseQaService.getById(qaId);
            if (qa == null) {
                return R.error("提问不存在");
            }

            // 校验课程归属
            CourseInfo course = courseInfoService.getById(qa.getCourseId());
            if (course == null || !teacherId.equals(course.getTeacherId())) {
                return R.error("无权回复该提问");
            }

            // 3. 更新回复
            qa.setAnswer(answer.trim());
            qa.setTeacherId(teacherId);
            qa.setAnswerTime(LocalDateTime.now());
            // ⭐ 确保审核状态为通过（如果之前是其他状态）
            qa.setAuditStatus("PASS");

            courseQaService.updateById(qa);

            return R.success("回复成功");

        } catch (Exception e) {
            System.err.println("Reply question error: " + e.getMessage());
            return R.error("回复失败");
        }
    }

    /**
     * ⭐ 核心方法：为提问创建审核记录
     */
    private void createAuditLogForQa(Integer qaId, Integer studentId) {
        AuditLog audit = new AuditLog();
        audit.setTargetType("QA");              // ⭐ 固定类型：课程问答
        audit.setTargetId(qaId);                // ⭐ 关联提问记录 ID
        audit.setResult("PENDING");             // ⭐ 初始状态：待审核
        audit.setAuditTime(LocalDateTime.now()); // ⭐ 创建时间
        auditLogService.save(audit);
    }


}
