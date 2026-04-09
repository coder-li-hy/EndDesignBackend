package com.reggie.reg.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.dto.CourseDTO;
import com.reggie.reg.entity.CourseInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 课程信息表 服务类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
public interface ICourseInfoService extends IService<CourseInfo> {

//    CourseInfo createCourse(Integer userId, CourseDTO dto);
    /**
     * 根据课程名称模糊查询（不分页）
     * @param courseName 课程名称关键词
     * @return 匹配的课程列表
     */
    List<CourseInfo> listByCourseName(String courseName);

    /**
     * 根据课程名称模糊查询 + 分页
     * @param courseName 课程名称关键词
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    Page<CourseInfo> pageByCourseName(String courseName, long page, long size);

    /**
     * 多条件组合查询（课程名称 + 状态 + 教师）
     */
    List<CourseInfo> listByCondition(String courseName, String status, Integer teacherId);
}
