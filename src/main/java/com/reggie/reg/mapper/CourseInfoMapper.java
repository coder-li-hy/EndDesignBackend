package com.reggie.reg.mapper;

import com.reggie.reg.entity.CourseInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 课程信息表 Mapper 接口
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
public interface CourseInfoMapper extends BaseMapper<CourseInfo> {
    /**
     * 批量更新过期课程状态
     * 🔥 关键：在 end_time 字段加索引 + 只更新未结束的课程
     */
    @Update("UPDATE course_info SET status = #{newStatus}" +
            "WHERE status = #{oldStatus} " +
            "AND end_date <= #{currentTime} ")
    int updateStatusByEndTime(
            @Param("oldStatus") String oldStatus,
            @Param("newStatus") String newStatus,
            @Param("currentTime") LocalDate currentTime
    );

}
