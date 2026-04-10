package com.reggie.reg.dto;

import com.reggie.reg.dto.CourseSimpleVO;
import com.reggie.reg.dto.UserDetailVO;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业 - 完整详情视图对象
 */
@Data
public class AssignmentDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer assignmentId;
    private String title;             // 作业标题
    private String description;       // 作业要求（完整内容）

    // 时间要求
    private LocalDateTime publishTime;
    private LocalDateTime deadline;
    private Boolean allowLate;        // 是否允许迟交

    // 评分规则
    private BigDecimal maxScore;      // 满分
    private String gradingRule;       // 评分标准描述

    // 关联信息
    private CourseSimpleVO course;    // 所属课程
    private UserDetailVO publisher;   // 发布教师

    // 统计信息
    private Integer totalSubmit;      // 总提交人数
    private Integer gradedCount;      // 已批改人数
    private BigDecimal avgScore;      // 平均分

    // 附件信息（如果有）
    private String attachmentUrl;
    private String attachmentName;
}