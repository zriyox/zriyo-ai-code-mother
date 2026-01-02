package com.zriyo.aicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zriyo.aicodemother.model.entity.AiToolLog;
import com.zriyo.aicodemother.model.vo.AiToolLogVo;

import java.util.List;

/**
 * AI 工具调用记录表 映射层。
 *
 */
public interface AiToolLogMapper extends BaseMapper<AiToolLog> {

    List<AiToolLogVo> slectList(Long massageId);

}
