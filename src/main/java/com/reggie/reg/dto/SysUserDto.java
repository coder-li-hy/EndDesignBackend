//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.reggie.reg.dto;

import com.reggie.reg.entity.SysUser;
import lombok.Data;

/**
 * 系统用户数据传输对象(SysUserDto)类
 * 继承自SysUser类，用于在系统各层之间传输用户数据
 * 使用@Data注解，该注解通常来自Lombok库，用于自动生成getter、setter等方法
 */
@Data
public class SysUserDto extends SysUser {
    // 用户密码字段
    private String password;
    // 用户旧密码字段，通常用于密码修改场景
    private String oldpassword;
    // 用户新密码字段，通常用于密码修改场景
    private String newpassword;
}
