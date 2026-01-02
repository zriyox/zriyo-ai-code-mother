// com.zriyo.aicodemother.service.sign.SignService
package com.zriyo.aicodemother.service;

import com.zriyo.aicodemother.model.enums.PointsReasonEnum;
import com.zriyo.aicodemother.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SignService {

    private final PointsAdjustService pointsAdjustService;
    private static final String SIGN_BITMAP_KEY_PREFIX = "sign:userId:";

    public Boolean signIn(Long userId) {
        LocalDate today = LocalDate.now();
        String key = buildSignKey(userId, today);
        int offset = today.getDayOfMonth() - 1;

        if (RedisUtils.getBit(key, offset)) {
            return false;
        }

        RedisUtils.setBit(key, offset, true);
        RedisUtils.expire(key, Duration.ofDays(365));

        pointsAdjustService.adjustPoints(userId, PointsReasonEnum.DAILY_SIGN, null, null);
        return true;
    }

    public boolean isSignedToday(Long userId) {
        LocalDate today = LocalDate.now();
        String key = buildSignKey(userId, today);
        int offset = today.getDayOfMonth() - 1;
        return RedisUtils.getBit(key, offset);
    }

    private String buildSignKey(Long userId, LocalDate date) {
        return SIGN_BITMAP_KEY_PREFIX + userId + ":" +
                date.getYear() + String.format("%02d", date.getMonthValue());
    }
}
