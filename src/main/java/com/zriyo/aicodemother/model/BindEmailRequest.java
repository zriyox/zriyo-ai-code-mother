package com.zriyo.aicodemother.model;

import lombok.Data;

@Data
public class BindEmailRequest {
    private String email;
    private String code;
}
