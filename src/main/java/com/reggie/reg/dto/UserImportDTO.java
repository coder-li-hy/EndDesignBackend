// dto/UserImportDTO.java
package com.reggie.reg.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用户导入数据封装类（对应 CSV 列）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportDTO {

    private String username;      // 用户名（必填）
    private String password;      // 密码（可选，空则用默认 123456）
    private String role;          // 角色：TEACHER/STUDENT（必填）
    private String email;         // 邮箱（可选）
    private String phone;         // 手机号（可选）
    private String status;        // 状态：ACTIVE/DISABLED（可选，默认 ACTIVE）

    /**
     * 校验单行数据
     */
    public String validate() {
        if (username == null || username.trim().isEmpty()) {
            return "用户名不能为空";
        }
        if (username.length() < 3 || username.length() > 20) {
            return "用户名长度应在 3-20 个字符";
        }
        if (!"TEACHER".equals(role) && !"STUDENT".equals(role)) {
            return "角色只能是 TEACHER 或 STUDENT";
        }
        if (email != null && !email.isEmpty() && !email.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "邮箱格式不正确: " + email;
        }
        if (phone != null && !phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
            return "手机号格式不正确: " + phone;
        }
        return null;  // 校验通过
    }
}