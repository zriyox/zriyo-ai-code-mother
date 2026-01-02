package com.zriyo.aicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.mapper.AiCodeGenRecordMapper;
import com.zriyo.aicodemother.model.entity.AiCodeGenRecord;
import com.zriyo.aicodemother.service.AiCodeGenRecordService;
import org.springframework.stereotype.Service;

/**
 * AI代码生成调用记录表 服务层实现。
 *
 */
@Service
public class AiCodeGenRecordServiceImpl extends ServiceImpl<AiCodeGenRecordMapper, AiCodeGenRecord>  implements AiCodeGenRecordService {

}
