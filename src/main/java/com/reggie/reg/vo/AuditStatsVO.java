// 1. AuditStatsVO.java - 审核统计
package com.reggie.reg.vo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditStatsVO implements Serializable {
    private Long pending;   // 待审核
    private Long passed;    // 已通过
    private Long rejected;  // 已拒绝
}
