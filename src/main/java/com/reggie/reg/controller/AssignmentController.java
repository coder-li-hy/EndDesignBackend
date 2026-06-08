package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.dto.GradeDTO;
import com.reggie.reg.entity.Assignment;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.Submission;
import com.reggie.reg.entity.SysUser;
import com.reggie.reg.service.IAssignmentService;
import com.reggie.reg.service.ICourseInfoService;
import com.reggie.reg.service.ISubmissionService;
import com.reggie.reg.service.ISysUserService;
import com.reggie.reg.vo.SubmissionVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作业表 前端控制器 作业在设计中不受内容审核管控
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
public class AssignmentController {
    private final ICourseInfoService courseInfoService;
    private final IAssignmentService assignmentService;
    private final ISubmissionService submissionService;
    private final ISysUserService sysUserService;

    /**
     * 1. 获取教师作业列表（按课程过滤）
     * GET /teacher/assignments?courseId=1&title=&page=1&size=10
     */
    @GetMapping("/teacher/assignments")
    public R<Page<Assignment>> listAssignments(
            @RequestParam Integer courseId,  // 必须传课程 ID
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String deadline,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        // 权限校验：确保是当前教师自己的课程
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        // 角色身份校验 先获取
        String role = (String) request.getSession().getAttribute("sys_user_role");
        // 如果当前登录角色并非教师
        if (!"TEACHER".equals(role)) {
            return R.error("无权查看");
        }
        // 校验 courseId 是否属于该教师
        CourseInfo course = courseInfoService.getById(courseId);
        if (course == null) {
            return R.error("课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            // 安全提示：不要返回"课程不属于您"，避免枚举课程 ID
            return R.error("无权访问该课程及其作业");
        }

        // /构造查询作业列表条件
        LambdaQueryWrapper<Assignment> query = new LambdaQueryWrapper<>();
        query.eq(Assignment::getCourseId, courseId);  // 按课程过滤
        // 根据标题模糊查询 如果输入的查询字段不为空 根据标题进行模糊查询
        query.like(StringUtils.isNotBlank(title), Assignment::getTitle, title);
        query.orderByDesc(Assignment::getAssignmentId);

        return R.success(assignmentService.page(new Page<>(page, size), query));
    }

    /**
     * 2. 发布作业
     * POST /teacher/assignments
     */
    @PostMapping("/teacher/assignments")
    public R<String> createAssignment(@RequestBody Assignment assignment, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        // 校验：作业所属课程的教师必须是当前用户
        // TODO: 根据 courseId 查询课程，验证 teacherId
        CourseInfo course = courseInfoService.getById(assignment.getCourseId());
        if (course == null) {
            return R.error("课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            return R.error("无权访问该课程");
        }
        assignmentService.save(assignment);
        return R.success("作业发布成功");
    }

    /**
     * 3. 更新作业
     * PUT /teacher/assignments/{assignmentId}
     */
    @PutMapping("/teacher/assignments/{assignmentId}")
    public R<String> updateAssignment(@PathVariable Integer assignmentId,
                                      @RequestBody Assignment dto,
                                      HttpServletRequest request) {
        // 权限校验 + 业务逻辑...
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        Assignment assignment = assignmentService.getById(assignmentId);

        // 校验：作业所属课程的教师必须是当前用户
        // TODO: 根据 courseId 查询课程，验证 teacherId
        CourseInfo course = courseInfoService.getById(assignment.getCourseId());
        if (course == null) {
            return R.error("课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            return R.error("无权访问该课程");
        }
        assignmentService.updateById(dto);
        return R.success("作业更新成功");
    }

    /**
     * 4. 删除作业
     * DELETE /teacher/assignments/{assignmentId}
     */
    @DeleteMapping("/teacher/assignments/{assignmentId}")
    public R<String> deleteAssignment(@PathVariable Integer assignmentId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        Assignment assignment = assignmentService.getById(assignmentId);
        if (assignment == null) return R.error("作业不存在");

        // 权限校验
        CourseInfo course = courseInfoService.getById(assignment.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权删除该作业");
        }

        // 先检查是否有学生提交，避免误删
        long submitCount = submissionService.count(
                new LambdaQueryWrapper<Submission>().eq(Submission::getAssignmentId, assignmentId)
        );
        if (submitCount > 0) {
            return R.error("该作业已有学生提交，无法删除");
        }
        // 如果没有学生提交作业 则可以删除
        assignmentService.removeById(assignmentId);
        return R.success("删除成功");
    }

    /**
     * 5. 查看某作业的提交列表
     * GET /teacher/assignments/{assignmentId}/submissions
     */
    @GetMapping("/teacher/assignments/{assignmentId}/submissions")
    public R<List<SubmissionVO>> getSubmissions(@PathVariable Integer assignmentId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        // 校验作业归属
        Assignment assignment = assignmentService.getById(assignmentId);
        if (assignment == null) return R.error("作业不存在");
        CourseInfo course = courseInfoService.getById(assignment.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权查看该作业的提交");
        }
        // 查询提交记录 + 关联学生姓名
        List<Submission> submissions = submissionService.list(
                new LambdaQueryWrapper<Submission>()
                        // 查询已经审核通过的提交记录
                        .eq(Submission::getAssignmentId, assignmentId).eq(Submission::getAuditStatus, "PASS")
                        .orderByDesc(Submission::getSubmitTime)
        );

        List<SubmissionVO> voList = submissions.stream().map(sub -> {
            SubmissionVO vo = new SubmissionVO();
            BeanUtils.copyProperties(sub, vo);
            // 关联学生姓名
            SysUser student = sysUserService.getById(sub.getStudentId());
            vo.setStudentName(student != null ? student.getUsername() : "未知学生");
            return vo;
        }).collect(Collectors.toList());

        return R.success(voList);
    }

    /**
     * 6. 批改作业（打分 + 评语）
     * PUT /teacher/submissions/{submissionId}/grade
     */
    @PutMapping("/teacher/submissions/{submissionId}/grade")
    public R<String> gradeSubmission(@PathVariable Integer submissionId,
                                     @RequestBody GradeDTO dto) {
        // 根据提交记录ID获取提交记录
        Submission submission = submissionService.getById(submissionId);
        // 判断提交记录是否存在，若不存在则返回错误信息
        if (submission == null) return R.error("提交记录不存在");

        // 设置提交记录的分数
        submission.setScore(dto.getScore());
        // 设置提交记录的教师评语
        submission.setTeacherComment(dto.getTeacherComment());
        // 设置批改时间为当前时间
        submission.setGradeTime(LocalDateTime.now());
        // 更新提交记录信息
        submissionService.updateById(submission);
        // 返回批改成功的响应信息
        return R.success("批改成功");
    }

    /**
     * 获取作业学习进度统计 + 提交列表
     * GET /teacher/assignments/{assignmentId}/progress
     */
    @GetMapping("/teacher/assignments/{assignmentId}/progress")
    public R<Map<String, Object>> getAssignmentProgress(
            @PathVariable Integer assignmentId,
            HttpServletRequest request) {

        try {
            // 1. 权限校验
            Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
            if (teacherId == null) {
                return R.error("未登录");
            }

            // 2. 查作业信息
            Assignment assignment = assignmentService.getById(assignmentId);
            if (assignment == null) {
                return R.error("作业不存在");
            }

            // 校验作业是否属于当前教师
            CourseInfo course = courseInfoService.getById(assignment.getCourseId());
            if (course == null || !teacherId.equals(course.getTeacherId())) {
                return R.error("无权访问该作业");
            }

            Integer courseId = assignment.getCourseId();  // 正确获取 courseId

            // 3. 查课程信息（获取选课人数 = 应交份数）
            int total = course.getCurrentCount() != null ? course.getCurrentCount() : 0;

            if (total >= course.getMaxCapacity()) {
                total = course.getMaxCapacity();
            }

            // 4. 查该作业的提交记录
            List<Submission> submissions = submissionService.list(
                    new LambdaQueryWrapper<Submission>()
                            .eq(Submission::getAssignmentId, assignmentId)
                            .orderByDesc(Submission::getSubmitTime)
            );

            // 5. 统计指标
            int submitted = submissions.size();
            // 修复：Boolean 比较用 .equals() 或布尔解包
            long lateCount = submissions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsLate()))
                    .count();
            long passCount = submissions.stream()
                    .filter(s -> s.getScore() != null && s.getScore().compareTo(new BigDecimal("60")) >= 0)
                    .count();

            double lateRate = submitted > 0 ? Math.round((double) lateCount / submitted * 100) : 0;
            double passRate = submitted > 0 ? Math.round((double) passCount / submitted * 100) : 0;

            // 6. 预加载学生姓名
            List<Integer> studentIds = submissions.stream()
                    .map(Submission::getStudentId)
                    .distinct().collect(Collectors.toList());
            Map<Integer, String> studentNameMap = new HashMap<>();
            if (!studentIds.isEmpty()) {
                sysUserService.listByIds(studentIds).forEach(u ->
                        studentNameMap.put(u.getUserId(), u.getUsername()));
            }

            // 7. 组装提交列表
            List<Map<String, Object>> submissionList = submissions.stream().map(s -> {
                Map<String, Object> item = new HashMap<>();
                item.put("submissionId", s.getSubmissionId());
                item.put("studentName", studentNameMap.get(s.getStudentId()));
                item.put("submitTime", s.getSubmitTime());
                item.put("isLate", s.getIsLate());
                item.put("score", s.getScore());
                item.put("teacherComment", s.getTeacherComment());
                item.put("lateReason", s.getLateReason());
                return item;
            }).collect(Collectors.toList());

            // 8. 返回结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("submitted", submitted);
            stats.put("lateRate", lateRate);
            stats.put("passRate", passRate);

            result.put("stats", stats);
            result.put("submissions", submissionList);

            return R.success(result);

        } catch (Exception e) {
            System.err.println("Progress query error: " + e.getMessage());
            // 容错：返回空数据
            Map<String, Object> empty = new HashMap<>();
            empty.put("stats", new HashMap<>());
            empty.put("submissions", new ArrayList<>());
            return R.success(empty);
        }
    }
}
