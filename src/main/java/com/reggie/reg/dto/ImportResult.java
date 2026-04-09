package com.reggie.reg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportResult {
    private Integer successCount;  // 成功条数
    private Integer failCount;     // 失败条数
    private List<String> failMessages;  // 失败原因列表
}