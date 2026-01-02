package com.zriyo.aicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.mapper.AiToolLogMapper;
import com.zriyo.aicodemother.model.entity.AiToolLog;
import com.zriyo.aicodemother.model.vo.AiToolLogVo;
import com.zriyo.aicodemother.service.AiToolLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 工具调用记录表 服务层实现。
 *
 */
@Service
@Slf4j
public class AiToolLogServiceImpl extends ServiceImpl<AiToolLogMapper, AiToolLog>  implements AiToolLogService{

    @Autowired
    private AiToolLogMapper aiToolLogMapper;

    @Override
    public void addAiToolLog(List<AiToolLog> toolLogs, Long aiMessageId) {
        try {
            if (toolLogs.isEmpty()){
                throw new RuntimeException("工具为空,调用失效");
            }
            boolean saveBatch = this.saveBatch(toolLogs);
        } catch (Exception e) {
            log.error("保存工具日志失败,失败 id :{},失败原因:{}", aiMessageId,e.getMessage());
        }

    }

    @Override
    public List<AiToolLogVo> selectToolLogs(Long massageId) {
        List<AiToolLogVo> vo = aiToolLogMapper.slectList(massageId);
        return vo;
    }
}
