package com.zriyo.aicodemother.wechatlogin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WeChatUserInfo {
    private String openid;
    private String nickname;
    private Integer sex;
    private String province;
    private String city;
    private String country;
    @JsonProperty("headimgurl")
    private String headImgUrl;
    private String unionid;
    private Integer errcode;
    private String errmsg;
}
