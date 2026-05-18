package com.reggie.reg.dto;

import lombok.Data;
import java.util.List;

/**
 * 迟交理由分析请求参数
 */
@Data
public class LateReasonAnalyzeRequest {
    /**
     * 迟交学生提交的原始理由列表
     * 示例：["家里有事", "电脑坏了没写完", "忘记截止时间了", "生病了"]
     */
    private List<String> reasons;

    /**
     * 期望的分类标签（可选，不提供则让 AI 自动归纳）
     */
    private List<String> expectedCategories;
}