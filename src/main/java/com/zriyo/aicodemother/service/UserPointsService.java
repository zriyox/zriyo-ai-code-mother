// com.zriyo.aicodemother.service.UserPointsService
package com.zriyo.aicodemother.service;

import com.zriyo.aicodemother.model.vo.UserPointsVo;

public interface UserPointsService {
    UserPointsVo getUserPointsVo(Long userId);
    Long getUserRank(Long userId);
}
