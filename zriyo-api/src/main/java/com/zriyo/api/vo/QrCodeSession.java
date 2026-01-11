package com.zriyo.api.vo;

import lombok.Data;

@Data
public class QrCodeSession {

    private String state;
    private String qrcodeId;
    private LoginUserVO user;
}
