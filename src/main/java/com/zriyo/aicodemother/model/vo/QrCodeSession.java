package com.zriyo.aicodemother.model.vo;

import lombok.Data;

@Data
public class QrCodeSession {

    private String state;
    private String qrcodeId;
    private LoginUserVO user;
}
