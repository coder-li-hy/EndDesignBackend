package com.reggie.reg.vo;

import com.reggie.reg.entity.Submission;
import lombok.Data;

// SubmissionVO.java - 提交列表展示用
@Data
public class SubmissionVO extends Submission {
    private String studentName;  // 关联的学生姓名
}