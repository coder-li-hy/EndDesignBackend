package com.reggie.reg.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public
class SubmissionSimpleVO implements Serializable {
    private Integer submissionId;
    private String assignmentTitle;
    private String contentType;
    private String filePath;
    private String textContent;
    private Boolean isLate;
    private String score;
    private String submitTime;
}