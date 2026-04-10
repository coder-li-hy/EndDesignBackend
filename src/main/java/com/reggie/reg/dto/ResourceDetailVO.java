package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程资源 - 完整详情视图对象
 */
@Data
public class ResourceDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 基础字段（同 SimpleVO）==========
    private Integer resourceId;
    private String title;
    private String type;              // PPT/VIDEO/LINK/FILE
    private String fileUrl;

    // ========== 扩展字段：更详细的关联信息 ==========

    /**
     * 所属课程完整信息
     */
    private CourseSimpleVO course;

    /**
     * 上传教师完整信息
     */
    private UserDetailVO uploader;

    /**
     * 资源描述/简介（如果有）
     */
    private String description;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件 MIME 类型（用于前端预览）
     * 示例：application/pdf, video/mp4
     */
    private String fileMimeType;

    /**
     * 下载次数/浏览次数（用于统计）
     */
    private Integer viewCount;
    private Integer downloadCount;

    /**
     * 审核状态变更历史
     */
    private String auditHistory;      // 示例："PENDING→PASS→REJECT→PASS"

    // 时间信息
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}