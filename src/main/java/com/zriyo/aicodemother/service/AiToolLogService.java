package com.zriyo.aicodemother.service;

import com.mybatisflex.core.service.IService;
import com.zriyo.aicodemother.model.entity.AiToolLog;
import com.zriyo.aicodemother.model.vo.AiToolLogVo;

import java.util.List;

/**
 * AI 工具调用记录表 服务层。
 *
 */
public interface AiToolLogService extends IService<AiToolLog> {

    /**
     * 添加工具调用记录 关联对话 Id
     * @param aiMessageId 对话 Id
     * @param toolLogs 工具调用记录
     */
    void addAiToolLog(List<AiToolLog> toolLogs, Long aiMessageId);


    List<AiToolLogVo> selectToolLogs(Long massageId);

}
