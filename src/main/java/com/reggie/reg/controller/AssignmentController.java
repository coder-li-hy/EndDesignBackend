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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 作业表 前端控制器
 * </p>
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
     * GET /api/teacher/assignments?courseId=1&title=&page=1&size=10
     */
    @GetMapping("/teacher/assignments")
    public R<Page<Assignment>> listAssignments(
            @RequestParam Integer courseId,  // ⭐ 必须传课程 ID
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String deadline,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        // 权限校验：确保是当前教师自己的课程
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        // TODO: 校验 courseId 是否属于该教师
        CourseInfo course = courseInfoService.getById(courseId);
        if (course == null) {
            return R.error("课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            // ⚠️ 安全提示：不要返回"课程不属于您"，避免枚举课程 ID
            return R.error("无权访问该课程");
        }




        LambdaQueryWrapper<Assignment> query = new LambdaQueryWrapper<>();
        query.eq(Assignment::getCourseId, courseId);  // ⭐ 按课程过滤
        query.like(StringUtils.isNotBlank(title), Assignment::getTitle, title);
        query.orderByDesc(Assignment::getAssignmentId);

        return R.success(assignmentService.page(new Page<>(page, size), query));
    }

    /**
     * 2. 发布作业
     * POST /api/teacher/assignments
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
            // ⚠️ 安全提示：不要返回"课程不属于您"，避免枚举课程 ID
            return R.error("无权访问该课程");
        }


        assignmentService.save(assignment);
        return R.success("作业发布成功");
    }

    /**
     * 3. 更新作业
     * PUT /api/teacher/assignments/{assignmentId}
     */
    @PutMapping("/teacher/assignments/{assignmentId}")
    public R<String> updateAssignment(@PathVariable Integer assignmentId,
                                      @RequestBody Assignment dto,
                                      HttpServletRequest request) {
        // 权限校验 + 业务逻辑...
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");
        Assignment assignment=assignmentService.getById(assignmentId);

        // 校验：作业所属课程的教师必须是当前用户
        // TODO: 根据 courseId 查询课程，验证 teacherId
        CourseInfo course = courseInfoService.getById(assignment.getCourseId());
        if (course == null) {
            return R.error("课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            // ⚠️ 安全提示：不要返回"课程不属于您"，避免枚举课程 ID
            return R.error("无权访问该课程");
        }
        assignmentService.updateById(dto);
        return R.success("作业更新成功");
    }

    /**
     * 4. 删除作业
     * DELETE /api/teacher/assignments/{assignmentId}
     */
    @DeleteMapping("/teacher/assignments/{assignmentId}")
    public R<String> deleteAssignment(@PathVariable Integer assignmentId,HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        Assignment assignment = assignmentService.getById(assignmentId);
        if (assignment == null) return R.error("作业不存在");

        // ✅ 权限校验
        CourseInfo course = courseInfoService.getById(assignment.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权删除该作业");
        }

        // 可选：先检查是否有学生提交，避免误删
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
     * GET /api/teacher/assignments/{assignmentId}/submissions
     */
    @GetMapping("/teacher/assignments/{assignmentId}/submissions")
    public R<List<SubmissionVO>> getSubmissions(@PathVariable Integer assignmentId,HttpServletRequest request) {
        Integer teacherId = (Integer) request.getSession().getAttribute("sys_user");

        // ✅ 校验作业归属
        Assignment assignment = assignmentService.getById(assignmentId);
        if (assignment == null) return R.error("作业不存在");

        CourseInfo course = courseInfoService.getById(assignment.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            return R.error("无权查看该作业的提交");
        }

        // 查询提交记录 + 关联学生姓名
        List<Submission> submissions = submissionService.list(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, assignmentId)
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
     * PUT /api/teacher/submissions/{submissionId}/grade
     */
    @PutMapping("/teacher/submissions/{submissionId}/grade")
    public R<String> gradeSubmission(@PathVariable Integer submissionId,
                                     @RequestBody GradeDTO dto) {
        Submission submission = submissionService.getById(submissionId);
        if (submission == null) return R.error("提交记录不存在");

        submission.setScore(dto.getScore());
        submission.setTeacherComment(dto.getTeacherComment());
        submission.setGradeTime(LocalDateTime.now());
        submissionService.updateById(submission);
        return R.success("批改成功");
    }
}
