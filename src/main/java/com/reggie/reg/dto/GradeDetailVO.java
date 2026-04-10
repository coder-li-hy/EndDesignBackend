package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业批改详情 - 视图对象
 */
@Data
public class GradeDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer gradeId;
    private BigDecimal score;             // 得分
    private String comment;               // 评语（完整）

    // 分项评分（如果有）
    private BigDecimal scoreCompleteness; // 完整性得分
    private BigDecimal scoreCorrectness;  // 正确性得分
    private BigDecimal scoreFormat;       // 格式规范得分

    // 批改人信息
    private UserDetailVO grader;

    // 批改时间
    private LocalDateTime gradeTime;

    // 是否允许学生查看
    private Boolean visibleToStudent;
}