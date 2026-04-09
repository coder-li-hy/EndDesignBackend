package com.reggie.reg.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchDeleteDTO {
    private List<Integer> userIds;
}