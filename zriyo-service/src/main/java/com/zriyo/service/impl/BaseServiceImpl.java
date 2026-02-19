package com.zriyo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zriyo.service.IBaseService;
import com.zriyo.service.mapper.BaseMapper;

/**
 * 通用 Service 实现类
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements IBaseService<T> {

}
