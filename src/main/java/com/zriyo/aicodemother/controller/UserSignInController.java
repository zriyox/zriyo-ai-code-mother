package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.service.UserSignInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户签到历史记录（逻辑外键，支持连续签到） 控制层。
 *
 */
@RestController
@RequestMapping("/userSignIn")
public class UserSignInController {

    @Autowired
    private UserSignInService userSignInService;



}
