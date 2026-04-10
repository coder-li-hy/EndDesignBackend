package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 课程信息 - 简化视图对象（用于关联展示）
 */
@Data
public class CourseSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer courseId;
    private String courseName;          // 课程名称
    private BigDecimal credits;         // 学分
    private String status;              // OPEN/CLOSED/ENDED
    private String statusText;          // 中文状态

    // 时间信息
    private LocalDate startDate;
    private LocalDate endDate;

    // 教师信息（简化）
    private UserSimpleVO teacher;

    // 人数信息
    private Integer maxCapacity;
    private Integer currentCount;
}