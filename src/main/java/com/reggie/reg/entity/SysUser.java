package com.reggie.reg.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Integer userId;

    /**
     * 账号
     */
    private String username;

    /**
     * 加密密码
     */
    private String passwordHash;

    /**
     * 角色
     */
    private String role;

    /**
     * 状态：正常/禁用
     */
    private String status;

    /**
     * 邮箱用于接收通知
     */
    private String email;

    private String phone;

    private LocalDateTime createTime;


}
