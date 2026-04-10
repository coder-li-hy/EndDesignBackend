package com.reggie.reg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// BatchRejectDTO.java
@Data
public class BatchRejectDTO extends BatchAuditDTO {
    @NotBlank(message = "拒绝原因不能为空")
    private String reason;
}