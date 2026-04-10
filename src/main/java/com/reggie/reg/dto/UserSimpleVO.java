package com.reggie.reg.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户信息 - 简化视图对象（用于展示，不包含敏感信息）
 */
@Data
public class UserSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 基础信息（只返回展示需要的）
    private Integer userId;         // 用于跳转用户详情页
    private String username;        // 用户名（核心展示字段）
    private String role;            // 角色：ADMIN/TEACHER/STUDENT
    private String roleText;        // 角色中文：管理员/教师/学生（前端直接显示用）

    // 可选：头像/邮箱（如果前端需要）
    // private String avatar;
    // private String email;

    // ⚠️ 绝对不要返回的敏感字段：
    // ❌ passwordHash（密码哈希）
    // ❌ phone（手机号，除非业务需要）
    // ❌ status（用户状态，审核列表不需要）
    // ❌ createTime（创建时间，与审核场景无关）
}