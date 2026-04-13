package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.Assignment;
import com.reggie.reg.entity.AuditLog;
import com.reggie.reg.entity.CourseSelection;
import com.reggie.reg.entity.Submission;
import com.reggie.reg.service.IAssignmentService;
import com.reggie.reg.service.IAuditLogService;
import com.reggie.reg.service.ICourseSelectionService;
import com.reggie.reg.service.ISubmissionService;
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
 * 作业提交表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
public class SubmissionController {

    private final IAssignmentService assignmentService;
    private final ISubmissionService submissionService;
    private final ICourseSelectionService courseSelectionService;
    private final IAuditLogService auditLogService;

    /**
     * 1. 获取课程作业列表（含学生提交状态）
     * GET /api/student/assignments?courseId=1&studentId=100
     */
    @GetMapping("/student/assignments")
    public R<List<Map<String, Object>>> getCourseAssignments(
            @RequestParam Integer courseId,
            @RequestParam Integer studentId,
            HttpServletRequest request) {

        try {
            // 1. 权限校验：学生只能查自己选的课程
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            // 校验课程是否已选（简化：查 course_selection）
            CourseSelection selection = courseSelectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)
                            .eq(CourseSelection::getStudentId, studentId)
                            .eq(CourseSelection::getStatus, "SELECTED")
            );
            if (selection == null) {
                return R.error("请先选课");
            }

            // 2. 查询该课程的作业列表
            List<Assignment> assignments = assignmentService.list(
                    new LambdaQueryWrapper<Assignment>()
                            .eq(Assignment::getCourseId, courseId)
                            .orderByDesc(Assignment::getDeadline)
            );

            if (assignments.isEmpty()) {
                return R.success(new ArrayList<>());
            }

            // 3. 查询该学生的提交记录
            List<Integer> assignmentIds = assignments.stream()
                    .map(Assignment::getAssignmentId).collect(Collectors.toList());

            Map<Integer, Submission> submissionMap = submissionService.list(
                    new LambdaQueryWrapper<Submission>()
                            .eq(Submission::getStudentId, studentId)
                            .in(Submission::getAssignmentId, assignmentIds)
            ).stream().collect(Collectors.toMap(Submission::getAssignmentId, s -> s));

            // 4. 组装结果
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Assignment assignment : assignments) {
                Map<String, Object> item = new HashMap<>();
                // 作业基本信息
                item.put("assignmentId", assignment.getAssignmentId());
                item.put("title", assignment.getTitle());
                item.put("description", assignment.getDescription());
                item.put("deadline", assignment.getDeadline());
                item.put("allowLate", assignment.getAllowLate());

                // 学生提交状态
                Submission submission = submissionMap.get(assignment.getAssignmentId());
                if (submission != null) {
                    Map<String, Object> subItem = new HashMap<>();
                    subItem.put("submissionId", submission.getSubmissionId());
                    subItem.put("auditStatus", submission.getAuditStatus());
                    subItem.put("score", submission.getScore());
                    subItem.put("teacherComment", submission.getTeacherComment());
                    subItem.put("contentType", submission.getContentType());
                    subItem.put("textContent", submission.getTextContent());
                    subItem.put("filePath", submission.getFilePath());
                    subItem.put("submitTime", submission.getSubmitTime());
                    subItem.put("isLate", submission.getIsLate());
                    item.put("mySubmission", subItem);
                }

                resultList.add(item);
            }

            return R.success(resultList);

        } catch (Exception e) {
            System.err.println("Get assignments error: " + e.getMessage());
            return R.success(new ArrayList<>());  // 容错
        }
    }

    /**
     * 2. 提交作业
     * POST /api/student/submissions
     */
    @PostMapping("/student/submissions")
    public R<String> submitAssignment(@RequestBody Map<String, Object> params, HttpServletRequest request) {

        try {
            // 1. 参数校验
            Integer assignmentId = (Integer) params.get("assignmentId");
            Integer studentId = (Integer) params.get("studentId");
            String contentType = (String) params.get("contentType");

            if (assignmentId == null || studentId == null || contentType == null) {
                return R.error("参数错误");
            }

            // 权限校验
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权操作");
            }

            // 2. 查作业信息
            Assignment assignment = assignmentService.getById(assignmentId);
            if (assignment == null) {
                return R.error("作业不存在");
            }

            // 3. 校验是否允许提交（简化：不强制校验截止时间，由 allowLate 控制）
            boolean isLate = false;
            String lateReason = null;
            if (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline())) {
                isLate = true;
                if (!Boolean.TRUE.equals(assignment.getAllowLate())) {
                    return R.error("该作业已截止，不允许迟交");
                }
                lateReason = (String) params.get("lateReason");
                if (lateReason == null || lateReason.trim().isEmpty()) {
                    return R.error("迟交作业必须填写迟交理由");
                }
            }

            // 4. 创建提交记录
            Submission submission = new Submission();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(studentId);
            submission.setContentType(contentType);

            if ("TEXT".equals(contentType)) {
                submission.setTextContent((String) params.get("textContent"));
            } else if ("FILE".equals(contentType)) {
                submission.setFilePath((String) params.get("filePath"));
            }

            submission.setSubmitTime(LocalDateTime.now());
            submission.setIsLate(isLate);
            if (isLate) {
                submission.setLateReason((String) params.get("lateReason"));
            }

            // ⭐ 审核状态：提交后进入待审核
            submission.setAuditStatus("PENDING");
             // TODO:向AuditLog中添加记录
            createAuditLogForSubmission(submission.getSubmissionId(), studentId);

            submissionService.save(submission);

            return R.success("提交成功，等待审核");

        } catch (Exception e) {
            System.err.println("Submit assignment error: " + e.getMessage());
            return R.error("提交失败");
        }
    }

    /**
     * 3. 查看我的提交记录（可选，用于刷新）
     * GET /api/student/submissions/my?studentId=100&assignmentId=1
     */
    @GetMapping("/student/submissions/my")
    public R<List<Map<String, Object>>> getMySubmissions(
            @RequestParam Integer studentId,
            @RequestParam(required = false) Integer assignmentId,
            HttpServletRequest request) {

        try {
            // 权限校验
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            LambdaQueryWrapper<Submission> query = new LambdaQueryWrapper<>();
            query.eq(Submission::getStudentId, studentId);
            if (assignmentId != null) {
                query.eq(Submission::getAssignmentId, assignmentId);
            }
            query.orderByDesc(Submission::getSubmitTime);

            List<Submission> submissions = submissionService.list(query);

            // 组装结果（简化）
            List<Map<String, Object>> resultList = submissions.stream().map(s -> {
                Map<String, Object> item = new HashMap<>();
                item.put("submissionId", s.getSubmissionId());
                item.put("assignmentId", s.getAssignmentId());
                item.put("auditStatus", s.getAuditStatus());
                item.put("score", s.getScore());
                item.put("teacherComment", s.getTeacherComment());
                item.put("contentType", s.getContentType());
                item.put("textContent", s.getTextContent());
                item.put("filePath", s.getFilePath());
                item.put("submitTime", s.getSubmitTime());
                item.put("isLate", s.getIsLate());
                return item;
            }).collect(Collectors.toList());

            return R.success(resultList);

        } catch (Exception e) {
            System.err.println("Get my submissions error: " + e.getMessage());
            return R.success(new ArrayList<>());
        }
    }
    /**
     * ⭐ 核心方法：为作业提交创建审核记录
     * @param submissionId 提交记录 ID
     * @param studentId 提交学生 ID（用于记录提交者）
     */
    private void createAuditLogForSubmission(Integer submissionId, Integer studentId) {
        AuditLog audit = new AuditLog();
        audit.setTargetType("SUBMISSION");        // ⭐ 固定类型：作业提交
        audit.setTargetId(submissionId);           // ⭐ 关联提交记录 ID
        audit.setResult("PENDING");                // ⭐ 初始状态：待审核
        audit.setAuditTime(LocalDateTime.now());   // ⭐ 创建时间
        // auditorId 和 reason 留空，等管理员审核时再填
        auditLogService.save(audit);
    }

}
