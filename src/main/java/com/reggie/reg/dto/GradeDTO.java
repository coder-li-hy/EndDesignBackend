package com.reggie.reg.dto;

import lombok.Data;

import java.math.BigDecimal;

// GradeDTO.java - 批改提交用
@Data
public class GradeDTO {
    private BigDecimal score;           // 分数 0-100
    private String teacherComment;      // 评语
}