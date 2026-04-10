package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 文件预览信息 - 视图对象
 * 用于前端判断如何预览文件
 */
@Data
public class FilePreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 文件基础信息
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;            // application/pdf, image/png 等

    // 预览方式建议（前端根据此字段决定如何展示）
    private String previewType;         // IMAGE/PDF/VIDEO/CODE/TEXT/DOWNLOAD_ONLY

    // 预览参数
    private Integer imageWidth;         // 如果是图片
    private Integer imageHeight;
    private Integer videoDuration;      // 如果是视频（秒）
    private String codeLanguage;        // 如果是代码文件

    // 安全信息
    private Boolean isSafe;             // 是否通过病毒扫描
    private String scanTime;            // 扫描时间
}