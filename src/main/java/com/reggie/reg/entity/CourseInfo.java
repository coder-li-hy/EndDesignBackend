package com.reggie.reg.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.ibatis.annotations.Insert;

/**
 * <p>
 * 课程信息表
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_info")
public class CourseInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "course_id", type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Integer courseId;

    /**
     * 授课教师 ID
     */
    private Integer teacherId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 学分使用字符串进行转化 保证数据精度
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal credits;

    /**
     * 最大容量
     */
    private Integer maxCapacity;

    /**
     * 当前人数
     */
    private Integer currentCount;

    /**
     * 课程状态
     */
    private String status;

    /**
     * 是否开放选课 1-是 0-否
     */
    private Boolean selectionOpen;

    /**
     * 开始日期 指定前端的输入格式
     */
    @JsonFormat(pattern = "yyyy-M-d", shape = JsonFormat.Shape.STRING)
    private LocalDate startDate;

    /**
     * 结束日期 指定前端的输入格式
     */
    @JsonFormat(pattern = "yyyy-M-d", shape = JsonFormat.Shape.STRING)
    private LocalDate endDate;


}
