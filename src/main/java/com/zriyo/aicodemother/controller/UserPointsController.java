package com.zriyo.aicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.PageRequest;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.model.vo.PointsLogPage;
import com.zriyo.aicodemother.model.vo.UserPointsVo;
import com.zriyo.aicodemother.service.CdkExchangeService;
import com.zriyo.aicodemother.service.PointsLogService;
import com.zriyo.aicodemother.service.SignService;
import com.zriyo.aicodemother.service.UserPointsService;
import com.zriyo.aicodemother.util.UserAuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户积分账户 控制层
 */
@RestController
@RequestMapping("/userPoints")
@RequiredArgsConstructor
public class UserPointsController {

    private final UserPointsService userPointsService;      // 仅用于查询 VO / 排名
    private final PointsLogService pointsLogService;        // 积分流水
    private final SignService signService;                  // 签到逻辑
    private final CdkExchangeService cdkExchangeService;    // CDK 兑换逻辑

    /**
     * 查询当前用户积分（含是否已签到）
     */
    @GetMapping("/get")
    public BaseResponse<UserPointsVo> getUserPoints() {
        Long userId = UserAuthUtil.getLoginId();
        return ResultUtils.success(userPointsService.getUserPointsVo(userId));
    }

    /**
     * 签到获取积分
     */
    @GetMapping("/sign")
    public BaseResponse<Boolean> signIn() {
        Long userId = UserAuthUtil.getLoginId();
        return ResultUtils.success(signService.signIn(userId));
    }

    /**
     * 兑换 CDK
     */
    @PostMapping("/exchange")
    public BaseResponse<Boolean> exchangeCdk(@RequestParam String cdk) {
        Long userId = UserAuthUtil.getLoginId();
        return ResultUtils.success(cdkExchangeService.exchangeCdk(userId, cdk));
    }

    /**
     * 获取积分历史
     */
    @PostMapping("/history")
    public BaseResponse<Page<PointsLogPage>> getHistory(@RequestBody PageRequest pageRequest) {
        Long userId = UserAuthUtil.getLoginId();
        return ResultUtils.success(pointsLogService.getUserPointsHistory(userId, pageRequest));
    }
}
