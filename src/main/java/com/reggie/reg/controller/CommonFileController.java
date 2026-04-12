package com.reggie.reg.controller;

import com.reggie.reg.common.R;
// ⭐ 必需 import
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// CommonFileController.java - 通用文件上传
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonFileController {

    @Value("${reggie.upload.path:E:/EndDesign/backend3/src/main/resources/file/upload}")
    private String uploadPath;

    /**
     * 文件上传接口
     * POST /api/common/upload
     */
    @PostMapping("/upload")
    public R<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error("文件不能为空");
        }

        try {
            // 1. 获取原始文件名
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return R.error("文件名不能为空");
            }

            // 2. 生成唯一文件名（存储用）
            String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            String uuidFileName = UUID.randomUUID() + ext;

            // 3. 保存文件
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(uuidFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. 返回结果
            Map<String, String> result = new HashMap<>();
            result.put("filePath", "/uploads/" + uuidFileName);  // 访问路径（相对路径，依赖 Nginx 映射）
            result.put("oriName", originalName);                  // 原始文件名（用于前端显示）

            log.info("文件上传成功: originalName={}, uuidFileName={}", originalName, uuidFileName);
            return R.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return R.error("文件上传失败：" + e.getMessage());
        }
    }
}