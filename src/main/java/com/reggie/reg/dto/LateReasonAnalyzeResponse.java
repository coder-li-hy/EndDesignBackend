package com.reggie.reg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 迟交理由分析响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LateReasonAnalyzeResponse {

    /**
     * 分类统计结果
     */
    private List<CategoryStat> categories;

    /**
     * 分析摘要（AI 生成的总结建议）
     */
    private String summary;

    /**
     * 单个分类统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String name;      // 分类名称：如"时间管理"
        private Integer value;    // 该分类的数量：如 35
        private String color;     // 前端图表颜色：如 "#667eea"
        private List<String> examples;  // 该分类下的典型理由示例
    }
}