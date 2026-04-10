package com.reggie.reg.dto;

import lombok.Data;

import java.util.List;

// BatchAuditDTO.java
@Data
public class BatchAuditDTO {
    private List<Integer> auditIds;
}