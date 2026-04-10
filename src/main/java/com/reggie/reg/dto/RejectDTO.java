package com.reggie.reg.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// RejectDTO.java
@Data
public class RejectDTO {
    @NotBlank(message = "拒绝原因不能为空")
    private String reason;
}