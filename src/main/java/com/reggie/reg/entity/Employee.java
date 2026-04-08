package com.reggie.reg.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String username;

    private String name;

    private String password;

    private String phone;

    private String sex;

    private String idNumber;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)  // 插入时填充字段
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时填充字段
    private LocalDateTime updateTime;

    //这个注解好高级，貌似是mybatis plus里面的某个
    //是自动填充啦
    @TableField(fill = FieldFill.INSERT)  // 插入时填充字段
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时填充字段
    private Long updateUser;

}
