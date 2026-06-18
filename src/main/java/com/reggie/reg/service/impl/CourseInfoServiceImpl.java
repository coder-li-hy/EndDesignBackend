package com.reggie.reg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.mapper.CourseInfoMapper;
import com.reggie.reg.service.ICourseInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Autowired
    private CourseInfoMapper courseMapper;
    /**
     * 根据课程名称查询课程信息列表
     *
     * @param courseName 课程名称，支持模糊查询
     * @return 符合条件的课程信息列表
     */
    @Override
    public List<CourseInfo> listByCourseName(String courseName) {
        // ⭐ LambdaQueryWrapper：类型安全，推荐写法
        // 创建查询条件构造器，用于构建查询条件
        LambdaQueryWrapper<CourseInfo> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊查询：course_name LIKE '%关键词%'
        // 如果课程名称不为空，则添加模糊查询条件
        if (StringUtils.isNotBlank(courseName)) {
            queryWrapper.like(CourseInfo::getCourseName, courseName);
        }

        // 可选：只查询开放中的课程
        // 如果需要查询开放状态的课程，可以取消注释以下代码
        // queryWrapper.eq(CourseInfo::getStatus, "OPEN");

        // 可选：按创建时间倒序
        // 如果需要按课程ID降序排列，可以取消注释以下代码
        // queryWrapper.orderByDesc(CourseInfo::getCourseId);

        // 执行查询并返回结果列表
        return this.baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据课程名称分页查询课程信息
     *
     * @param courseName 课程名称，可以为空
     * @param page       当前页码
     * @param size       每页显示数量
     * @return 返回分页后的课程信息结果
     */
    @Override
    public Page<CourseInfo> pageByCourseName(String courseName, long page, long size) {
        // 创建Lambda查询条件构造器
        LambdaQueryWrapper<CourseInfo> queryWrapper = new LambdaQueryWrapper<>();

        // 判断课程名称是否为空，如果不为空则添加模糊查询条件
        if (StringUtils.isNotBlank(courseName)) {
            queryWrapper.like(CourseInfo::getCourseName, courseName);
        }

        // 分页查询
        return this.baseMapper.selectPage(new Page<>(page, size), queryWrapper);
    }

    @Override
    /**
     * 根据条件查询课程信息列表
     * @param courseName 课程名称，模糊查询条件，可为空
     * @param status 课程状态，精确查询条件，可为空
     * @param teacherId 教师ID，精确查询条件，可为空
     * @return 符合条件的课程信息列表
     */
    public List<CourseInfo> listByCondition(String courseName, String status, Integer teacherId) {
        // 创建Lambda查询包装器，用于构建动态查询条件
        LambdaQueryWrapper<CourseInfo> queryWrapper = new LambdaQueryWrapper<>();

        // 多条件动态拼接，根据参数是否为空来决定是否添加查询条件
        // 当courseName不为空时，添加课程名称的模糊查询条件
        queryWrapper.like(StringUtils.isNotBlank(courseName),
                CourseInfo::getCourseName, courseName);
        // 当status不为空时，添加课程状态的精确查询条件
        queryWrapper.eq(StringUtils.isNotBlank(status),
                CourseInfo::getStatus, status);
        // 当teacherId不为空时，添加教师ID的精确查询条件
        queryWrapper.eq(teacherId != null,
                CourseInfo::getTeacherId, teacherId);

        // 执行查询并返回结果列表
        return this.baseMapper.selectList(queryWrapper);
    }

    /**
     * 更新已过期的课程状态为 ENDED
     * 只扫描 "开放中" 的课程 + 时间索引优化
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateExpiredToEnded() {
        // 使用数据库时间比较，避免查出大量数据到内存
        return courseMapper.updateStatusByEndTime(
                "OPEN",  // 只处理进行中的课程
                "ENDED",    // 更新为已结束
                LocalDate.now()              // 当前时间
        );
    }

}
