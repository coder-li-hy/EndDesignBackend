//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.reggie.reg.dto;

import com.reggie.reg.entity.SysUser;
import lombok.Data;

@Data
public class SysUserDto extends SysUser {
    private String password;
    private String oldpassword;
    private String newpassword;
}
