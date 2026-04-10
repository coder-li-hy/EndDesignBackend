package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 课程资源 - 简化视图对象（用于审核列表展示）
 */
@Data
public class ResourceSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 基础信息
    private Integer resourceId;
    private String title;           // 资源标题（前端显示用）
    private String type;            // 资源类型：PPT/VIDEO/LINK/FILE

    // 关联信息（只取展示需要的）
    private String courseName;      // 所属课程名称

    // 文件信息
    private String fileUrl;         // 文件链接（用于预览/下载）

    // 不需要返回的字段（对比 Resource 实体）：
    // ❌ courseId（前端不需要）
    // ❌ uploaderId（用 submitter 代替）
    // ❌ auditStatus（审核列表已有 result 字段）
    // ❌ createTime（审核列表已有 auditTime）
}