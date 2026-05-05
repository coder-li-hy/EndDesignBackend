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
     * 获取学生指定课程的作业列表
     * 该接口用于学生查看自己已选课程的作业信息，包括作业详情和自己的提交状态
     *
     * @param courseId 课程ID
     * @param studentId 学生ID
     * @param request HTTP请求对象，用于获取session信息
     * @return 返回作业列表，每个作业包含基本信息和学生的提交状态（如果有）
     */
    @GetMapping("/student/assignments")
    public R<List<Map<String, Object>>> getCourseAssignments(
            @RequestParam Integer courseId,    // 课程ID
            @RequestParam Integer studentId,  // 学生ID
            HttpServletRequest request) {     // HTTP请求对象

        try {
            // 1. 权限校验：学生只能查自己选的课程
            // 从session中获取当前登录用户ID
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            // 检查用户是否登录以及是否有权访问指定学生的作业
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            // 校验课程是否已选（简化：查 course_selection）
            // 查询学生是否已选择该课程且状态为已选
            CourseSelection selection = courseSelectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)    // 课程ID匹配
                            .eq(CourseSelection::getStudentId, studentId)  // 学生ID匹配
                            .eq(CourseSelection::getStatus, "SELECTED")    // 状态为已选
            );
            // 如果没有选课记录，返回错误提示
            if (selection == null) {
                return R.error("当前课程未被选中");
            }

            // 2. 查询该课程的作业列表
            // 按截止日期降序查询指定课程的所有作业
            List<Assignment> assignments = assignmentService.list(
                    new LambdaQueryWrapper<Assignment>()
                            .eq(Assignment::getCourseId, courseId)         // 课程ID匹配
                            .orderByDesc(Assignment::getDeadline)         // 按截止日期降序排列
            );

            // 如果没有作业，返回空列表
            if (assignments.isEmpty()) {
                return R.success(new ArrayList<>());
            }

            // 3. 查询该学生的提交记录
            // 提取所有作业ID
            List<Integer> assignmentIds = assignments.stream()
                    .map(Assignment::getAssignmentId).collect(Collectors.toList());

            // 查询该学生对这些作业的所有提交记录
            Map<Integer, Submission> submissionMap = submissionService.list(
                    new LambdaQueryWrapper<Submission>()
                            .eq(Submission::getStudentId, studentId)      // 学生ID匹配
                            .in(Submission::getAssignmentId, assignmentIds) // 作业ID在列表中
            ).stream().collect(Collectors.toMap(Submission::getAssignmentId, s -> s));

            // 4. 组装结果
            // 创建结果列表
            List<Map<String, Object>> resultList = new ArrayList<>();
            // 遍历每个作业
            for (Assignment assignment : assignments) {
                // 创建作业信息Map
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
     * 处理学生提交作业的请求
     * @param params 包含提交作业所需参数的Map，如作业ID、学生ID、内容类型等
     * @param request HttpServletRequest对象，用于获取会话信息
     * @return 返回操作结果，成功或失败信息
     */
    @PostMapping("/student/submissions")
    public R<String> submitAssignment(@RequestBody Map<String, Object> params, HttpServletRequest request) {

        try {
            // 1. 参数校验：检查必要参数是否存在
            Integer assignmentId = (Integer) params.get("assignmentId");  // 获取作业ID
            Integer studentId = (Integer) params.get("studentId");      // 获取学生ID
            String contentType = (String) params.get("contentType");    // 获取内容类型

            // 检查必要参数是否为空
            if (assignmentId == null || studentId == null || contentType == null) {
                return R.error("参数错误");
            }

            // 权限校验：验证当前用户是否有权限提交作业
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
     * 获取学生提交的作业列表
     * @param studentId 学生ID
     * @param assignmentId 作业ID（可选参数）
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回包含学生提交作业信息的列表，如果发生错误则返回空列表
     */
    @GetMapping("/student/submissions/my")
    public R<List<Map<String, Object>>> getMySubmissions(
            @RequestParam Integer studentId,        // 学生ID参数
            @RequestParam(required = false) Integer assignmentId,  // 可选的作业ID参数
            HttpServletRequest request) {           // HTTP请求对象

        try {
            // 权限校验：检查当前用户是否为请求的学生本人
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            // 构建查询条件：根据学生ID和可选的作业ID查询提交记录
            LambdaQueryWrapper<Submission> query = new LambdaQueryWrapper<>();
            query.eq(Submission::getStudentId, studentId);
            if (assignmentId != null) {
                query.eq(Submission::getAssignmentId, assignmentId);
            }
            query.orderByDesc(Submission::getSubmitTime);  // 按提交时间降序排列

            // 执行查询获取提交记录列表
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
     * 创建作业提交的审核日志
     * @param submissionId 提交记录ID
     * @param studentId 学生ID
     */
    private void createAuditLogForSubmission(Integer submissionId, Integer studentId) {
        AuditLog audit = new AuditLog();                       // 创建审核日志对象
        audit.setTargetType("SUBMISSION");        // 固定类型：作业提交
        audit.setTargetId(submissionId);           //  关联提交记录 ID
        audit.setResult("PENDING");                // 初始状态：待审核
        audit.setAuditTime(LocalDateTime.now());   // 创建时间
        // auditorId 和 reason 留空，等管理员审核时再填
        auditLogService.save(audit);                          // 保存审核日志
    }

}
