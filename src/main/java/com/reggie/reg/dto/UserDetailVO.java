package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息 - 完整详情视图对象（用于详情页）
 * 比 UserSimpleVO 包含更多展示字段，但仍排除敏感信息
 */
@Data
public class UserDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 基础信息 ==========
    private Integer userId;
    private String username;
    private String role;                // ADMIN/TEACHER/STUDENT
    private String roleText;            // 中文角色

    // ========== 展示信息（非敏感）==========
    private String email;               // 邮箱（业务需要时返回）
    private String phone;               // 手机号（业务需要时返回，可脱敏）
    private String avatar;              // 头像 URL

    // ========== 统计信息 ==========
    private Integer courseCount;        // 创建/参与的课程数
    private Integer resourceCount;      // 上传的资源数
    private LocalDateTime lastLoginTime; // 最后登录时间

    // ========== 状态信息 ==========
    private String status;              // ACTIVE/DISABLED
    private String statusText;          // 中文状态

    // 时间信息
    private LocalDateTime createTime;

    // ⚠️ 绝对不包含的字段：
    // ❌ passwordHash（密码哈希）
    // ❌ 其他敏感个人信息
}