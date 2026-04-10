package com.reggie.reg.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public
class UserSimpleVO implements Serializable {
    private Integer userId;
    private String username;
    private String role;
    private String roleText;  // 中文角色
}
