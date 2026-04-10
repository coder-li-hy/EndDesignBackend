package com.reggie.reg.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public
class QaSimpleVO implements Serializable {
    private Integer qaId;
    private String question;
    private String answer;
    private Boolean isAnonymous;
    private String courseName;
    private String askTime;
}
