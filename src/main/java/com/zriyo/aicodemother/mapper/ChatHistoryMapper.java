package com.zriyo.aicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zriyo.aicodemother.model.dto.chat.ChatHistoryQueryRequest;
import com.zriyo.aicodemother.model.entity.ChatHistory;
import com.zriyo.aicodemother.model.vo.ChatHistoryQueryVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话历史 映射层。
 *
 */
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {



    // ChatHistoryMapper.java
    List<ChatHistoryQueryVo> ChatHistoryQueryList(
            @Param("request") ChatHistoryQueryRequest request,
            @Param("userId") Long userId
    );

}
