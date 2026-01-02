package com.zriyo.aicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.mapper.UserSignInMapper;
import com.zriyo.aicodemother.model.entity.UserSignIn;
import com.zriyo.aicodemother.service.UserSignInService;
import org.springframework.stereotype.Service;

/**
 * 用户签到历史记录（逻辑外键，支持连续签到） 服务层实现。
 *
 */
@Service
public class UserSignInServiceImpl extends ServiceImpl<UserSignInMapper, UserSignIn>  implements UserSignInService {

}
