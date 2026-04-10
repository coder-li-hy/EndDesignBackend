package com.reggie.reg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// AuditStatsVO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditStatsVO {
    private Long pending;   // 待审核数量
    private Long passed;    // 已通过数量
    private Long rejected;  // 已拒绝数量
    private Long total;     // 总数量
}