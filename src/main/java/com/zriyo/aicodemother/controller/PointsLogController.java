package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.service.PointsLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 积分变动流水日志（逻辑外键，全量可追溯） 控制层。
 *
 */
@RestController
@RequestMapping("/pointsLog")
public class PointsLogController {

    @Autowired
    private PointsLogService pointsLogService;


}
