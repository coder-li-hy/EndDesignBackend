package com.reggie.reg.vo;

import lombok.Data;

import java.io.Serializable;

// 3. 简化 VO（用于列表展示，排除敏感字段）
@Data
public
class ResourceSimpleVO implements Serializable {
    private Integer resourceId;
    private String title;
    private String type;
    private String courseName;
    private String fileUrl;
}