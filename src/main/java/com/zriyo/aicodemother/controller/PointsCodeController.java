package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.service.PointsCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 积分兑换码管理表（逻辑外键，支持批量发放与追踪） 控制层。
 *
 */
@RestController
@RequestMapping("/pointsCode")
public class PointsCodeController {

    @Autowired
    private PointsCodeService pointsCodeService;


}
