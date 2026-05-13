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

    // 指定文件上传位置
    @Value("${reggie.upload.path:E:/EndDesign/backend3/src/main/resources/file/upload}")
    private String uploadPath;


    /**
     * 处理文件上传请求
     * @param file 上传的文件对象
     * @return 返回操作结果，包含文件信息或错误信息
     */
    @PostMapping("/upload")
    public R<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        // 检查文件是否为空
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
            // 获取文件扩展名并转换为小写
            String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            // 使用UUID生成唯一文件名，确保文件名不重复 加上ext后缀保证文件后缀不变
            String uuidFileName = UUID.randomUUID() + ext;

            // 3. 保存文件到指定目录
            // 获取上传目录路径
            Path uploadDir = Paths.get(uploadPath);
            // 如果目录不存在则创建
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            // 构建完整文件路径
            Path filePath = uploadDir.resolve(uuidFileName);
            // 复制文件到目标位置，如果文件已存在则替换
            //         获取文件输入流            文件完整路径 替换文件位置
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. 返回结果
            Map<String, String> result = new HashMap<>();
            result.put("filePath", "/upload/" + uuidFileName);  // 访问路径（相对路径，依赖 Nginx 映射）
            result.put("oriName", originalName);                  // 原始文件名（用于前端显示）

            log.info("文件上传成功: originalName={}, uuidFileName={}", originalName, uuidFileName);
            return R.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return R.error("文件上传失败：" + e.getMessage());
        }
    }
}