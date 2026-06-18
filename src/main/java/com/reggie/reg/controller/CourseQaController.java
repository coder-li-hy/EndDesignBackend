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
     * 获取学生问答列表接口
     * @param courseId 课程ID
     * @param studentId 学生ID
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回学生问答列表，包含提问和审核通过后的回复信息
     */
    @GetMapping("/student/qa")
    public R<List<Map<String, Object>>> getStudentQaList(
            @RequestParam Integer courseId,  // 课程ID参数
            @RequestParam Integer studentId,  // 学生ID参数
            HttpServletRequest request) {  // HTTP请求对象

        try {
            // 1. 权限校验：验证当前用户是否为请求的学生本人
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权访问");
            }

            // 校验课程是否已选：查询课程选择记录，确保学生已选择该课程
            CourseSelection selection = courseSelectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)  // 匹配课程ID
                            .eq(CourseSelection::getStudentId, studentId)  // 匹配学生ID
                            .eq(CourseSelection::getStatus, "SELECTED")  // 确保已选课状态
            );
            if (selection == null) {
                return R.error("请先选课");
            }

            // 2. 查询该学生的提问记录：按提问时间倒序排列 学生查看自身的提问不用加上审核通过限制
            List<CourseQa> qaList = courseQaService.list(
                    new LambdaQueryWrapper<CourseQa>()
                            .eq(CourseQa::getCourseId, courseId)  // 匹配课程ID
                            .eq(CourseQa::getStudentId, studentId)  // 匹配学生ID
                            .orderByDesc(CourseQa::getAskTime)  // 按提问时间降序排列
            );

            // 如果没有提问记录，返回空列表
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

                //仅审核通过后，才返回教师回复 如果审核未通过 其实教师也不会看到这条提问
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
     * 处理学生提问的接口
     * @param params 包含提问信息的参数Map，需包含courseId, studentId, question等字段
     * @param request HTTP请求对象，用于获取session中的用户信息
     * @return 返回操作结果，包含成功或失败信息
     */
    @PostMapping("/student/qa")
    public R<String> askQuestion(@RequestBody Map<String, Object> params, HttpServletRequest request) {

        try {
            // 使用安全转换方法
            Integer courseId = getIntegerParam(params, "courseId");
            Integer studentId = getIntegerParam(params, "studentId");
            String question = (String) params.get("question");

            // 验证参数是否为空或无效 提问.trim()消除了空格
            if (courseId == null || studentId == null || question == null || question.trim().isEmpty()) {
                return R.error("参数错误");
            }

            // 权限校验 - 验证当前用户是否为提问学生本人
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return R.error("无权操作");
            }

            // 2. 校验课程是否已选 - 检查学生是否已选择该课程
            CourseSelection selection = courseSelectionService.getOne(
                    new LambdaQueryWrapper<CourseSelection>()
                            .eq(CourseSelection::getCourseId, courseId)
                            .eq(CourseSelection::getStudentId, studentId)
                            .eq(CourseSelection::getStatus, "SELECTED")
            );
            if (selection == null) {
                return R.error("请先选课");
            }
            CourseInfo courseInfo=courseInfoService.getById(courseId);


            // 3. 创建提问记录 - 构建问答对象并设置属性
            CourseQa qa = new CourseQa();
            qa.setCourseId(courseId);
            qa.setStudentId(studentId);
            qa.setQuestion(question.trim());
            qa.setIsAnonymous(Boolean.TRUE.equals(params.get("isAnonymous")));
            qa.setAskTime(LocalDateTime.now());
            qa.setTeacherId(courseInfo.getTeacherId());

            // 设置审核状态为待审核
            qa.setAuditStatus("PENDING");

            // 提交课程问答 - 保存问答记录到数据库
            courseQaService.save(qa);

            // 创建课程问答审核记录 - 为新创建的问答生成审核日志
            createAuditLogForQa(qa.getQaId(), studentId);

            return R.success("提问成功，等待审核");

        } catch (Exception e) {
            // 捕获并打印异常信息
            System.err.println("Ask question error: " + e.getMessage());
            return R.error("提交失败");
        }
    }


    /**
     * 获取教师问答列表接口
     * @param courseId 课程ID
     * @param teacherId 教师ID
     * @param request HTTP请求对象，用于获取session中的用户信息
     * @return 返回问答列表结果，包含问题和回答信息
     */
    @GetMapping("/teacher/qa")
    public R<List<Map<String, Object>>> getTeacherQaList(
            @RequestParam Integer courseId,    // 课程ID参数
            @RequestParam Integer teacherId,   // 教师ID参数
            HttpServletRequest request) {      // HTTP请求对象

        try {
            // 1. 权限校验
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");  // 从session中获取当前登录用户ID
            if (currentUserId == null || !currentUserId.equals(teacherId)) {  // 检查用户是否登录且是否为指定教师
                return R.error("无权访问");  // 返回无权限错误信息
            }

            // 校验课程是否属于该教师 ?? 前端其实做了限制 但是后端可以保险一点
            CourseInfo course = courseInfoService.getById(courseId);  // 根据课程ID获取课程信息
            if (course == null || !teacherId.equals(course.getTeacherId())) {  // 检查课程是否存在且属于该教师
                return R.error("无权访问该课程");  // 返回无权限访问课程的错误信息
            }

            // 2. 查询该课程下审核通过的提
            List<CourseQa> qaList = courseQaService.list(  // 查询问答列表
                    new LambdaQueryWrapper<CourseQa>()
                            .eq(CourseQa::getCourseId, courseId)  // 按课程ID筛选
                            .eq(CourseQa::getAuditStatus, "PASS")  // 只显示审核通过的
                            .orderByDesc(CourseQa::getAskTime)  // 按提问时间降序排列
            );

            if (qaList.isEmpty()) {  // 如果查询结果为空
                return R.success(new ArrayList<>());  // 返回空列表
            }

            // 3. 预加载学生姓名（匿名提问不显示）
            List<Integer> studentIds = qaList.stream()  // 获取所有非匿名提问的学生ID
                    .filter(qa -> !Boolean.TRUE.equals(qa.getIsAnonymous()))
                    .map(CourseQa::getStudentId)
                    .distinct().collect(Collectors.toList());

            Map<Integer, String> studentNameMap = new HashMap<>();  // 创建学生ID到姓名的映射
            if (!studentIds.isEmpty()) {  // 如果有学生ID
                sysUserService.listByIds(studentIds).forEach(u ->  // 根据ID列表获取学生信息
                        studentNameMap.put(u.getUserId(), u.getUsername()));
            }

            // 4. 组装结果
            List<Map<String, Object>> resultList = qaList.stream().map(qa -> {  // 将问答列表转换为结果列表
                Map<String, Object> item = new HashMap<>();  // 创建结果项
                item.put("qaId", qa.getQaId());  // 问答ID
                item.put("question", qa.getQuestion());  // 问题内容
                item.put("isAnonymous", qa.getIsAnonymous());  // 是否匿名
                item.put("askTime", qa.getAskTime());  // 提问时间
                item.put("answer", qa.getAnswer());  // 回答内容
                item.put("answerTime", qa.getAnswerTime());  // 回答时间

                // 非匿名提问才显示学生姓名
                if (!Boolean.TRUE.equals(qa.getIsAnonymous())) {  // 如果不是匿名提问
                    item.put("studentName", studentNameMap.get(qa.getStudentId()));  // 添加学生姓名
                }

                return item;  // 返回结果项
            }).collect(Collectors.toList());  // 收集所有结果项

            return R.success(resultList);  // 返回成功结果

        } catch (Exception e) {  // 异常处理
            System.err.println("Get teacher QA error: " + e.getMessage());  // 打印错误信息
            return R.success(new ArrayList<>());  // 容错，返回空列表
        }
    }


    /**
     * 处理教师回复课程提问的接口
     * @param qaId 提问ID，路径变量
     * @param params 包含教师ID和回复内容的Map
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回操作结果，R类型封装，包含成功/失败信息
     */
    @PutMapping("/teacher/qa/{qaId}/answer")
    public R<String> replyQuestion(
            @PathVariable Integer qaId,  // 从路径中获取的提问ID
            @RequestBody Map<String, Object> params,  // 请求体中的参数，包含教师ID和回复内容
            HttpServletRequest request) {  // HTTP请求对象，用于获取当前登录用户信息

        try {
            // 1. 参数校验：检查必要参数是否存在
            Integer teacherId = (Integer) params.get("teacherId");  // 从参数中获取教师ID
            String answer = (String) params.get("answer");  // 从参数中获取回复内容

            // 验证提问ID、教师ID和回复内容是否为空或无效
            if (qaId == null || teacherId == null || answer == null || answer.trim().isEmpty()) {
                return R.error("参数错误");
            }

            // 权限校验：验证当前用户是否有权限进行此操作
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");  // 从会话中获取当前登录用户ID
            if (currentUserId == null || !currentUserId.equals(teacherId)) {  // 检查用户是否已登录且与请求的教师ID一致
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
            // 确保审核状态为通过（如果之前是其他状态） 本质教师回复学生问题不应该改变审核状态
            qa.setAuditStatus("PASS");

            courseQaService.updateById(qa);

            return R.success("回复成功");

        } catch (Exception e) {
            System.err.println("Reply question error: " + e.getMessage());
            return R.error("回复失败");
        }
    }

    /**
     * 安全获取 Integer 参数（兼容 String/Number/Integer）
     */
    private Integer getIntegerParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return null;

        // 已经是 Integer，直接返回
        if (value instanceof Integer) {
            return (Integer) value;
        }

        // 是字符串，尝试解析
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // 是其他数字类型（如 Long），转成 int
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return null;
    }


    /**
     * 创建课程问答的审核日志
     * @param qaId 课程问答记录ID
     * @param studentId 学生ID（当前方法中未使用）
     */
    private void createAuditLogForQa(Integer qaId, Integer studentId) {
        // 创建审核日志对象
        AuditLog audit = new AuditLog();
        // 设置审核目标类型为课程问答
        audit.setTargetType("QA");              // 固定类型：课程问答
        // 设置审核目标ID为问答记录ID
        audit.setTargetId(qaId);                // 关联提问记录 ID
        // 设置审核结果为待审核状态
        audit.setResult("PENDING");             // 初始状态：待审核
        // 设置审核时间为当前系统时间
        audit.setAuditTime(LocalDateTime.now()); // 创建时间
        // 保存审核日志记录
        auditLogService.save(audit);
    }


}
