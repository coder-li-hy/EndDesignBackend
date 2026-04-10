// AuditQueryDTO.java
package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 审核查询条件 - 数据传输对象
 */
@Data
public class AuditQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String targetType;   // RESOURCE/QA/SUBMISSION
    private String status;       // PENDING/PASS/REJECT
    private String keyword;      // 搜索关键词
    private Integer page;        // 页码
    private Integer size;        // 每页大小
}