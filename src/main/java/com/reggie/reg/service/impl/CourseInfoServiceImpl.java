package com.reggie.reg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.mapper.CourseInfoMapper;
import com.reggie.reg.service.ICourseInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 课程信息表 服务实现类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Service
public class CourseInfoServiceImpl extends ServiceImpl<CourseInfoMapper, CourseInfo> implements ICourseInfoService {
    @Override
    public List<CourseInfo> listByCourseName(String courseName) {
        // ⭐ LambdaQueryWrapper：类型安全，推荐写法
        LambdaQueryWrapper<CourseInfo> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊查询：course_name LIKE '%关键词%'
        if (StringUtils.isNotBlank(courseName)) {
            queryWrapper.like(CourseInfo::getCourseName, courseName);
        }

        // 可选：只查询开放中的课程
        // queryWrapper.eq(CourseInfo::getStatus, "OPEN");

        // 可选：按创建时间倒序
        // queryWrapper.orderByDesc(CourseInfo::getCourseId);

        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public Page<CourseInfo> pageByCourseName(String courseName, long page, long size) {
        LambdaQueryWrapper<CourseInfo> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(courseName)) {
            queryWrapper.like(CourseInfo::getCourseName, courseName);
        }

        // ⭐ 分页查询
        return this.baseMapper.selectPage(new Page<>(page, size), queryWrapper);
    }

    @Override
    public List<CourseInfo> listByCondition(String courseName, String status, Integer teacherId) {
        LambdaQueryWrapper<CourseInfo> queryWrapper = new LambdaQueryWrapper<>();

        // 多条件动态拼接
        queryWrapper.like(StringUtils.isNotBlank(courseName),
                CourseInfo::getCourseName, courseName);
        queryWrapper.eq(StringUtils.isNotBlank(status),
                CourseInfo::getStatus, status);
        queryWrapper.eq(teacherId != null,
                CourseInfo::getTeacherId, teacherId);

        return this.baseMapper.selectList(queryWrapper);
    }

}
