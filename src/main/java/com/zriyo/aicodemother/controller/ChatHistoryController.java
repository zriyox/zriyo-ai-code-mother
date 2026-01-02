package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.dto.chat.ChatHistoryQueryRequest;
import com.zriyo.aicodemother.model.vo.AiToolLogVo;
import com.zriyo.aicodemother.model.vo.ChatHistoryQueryVo;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.UserAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对话历史 控制层。
 *
 */
@RestController
@RequestMapping("/chatHistory")
@Validated
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private AiToolLogService aiToolLogService;

    /**
     * 游标查询 对话历史
     */
    @GetMapping("list")
    public BaseResponse<List<ChatHistoryQueryVo>> list(ChatHistoryQueryRequest request) {
        Long loginId = UserAuthUtil.getLoginId();
        List<ChatHistoryQueryVo> chatHistoryQueryVoPage = chatHistoryService
                .ChatHistoryQueryList(request, loginId);
        return ResultUtils.success(chatHistoryQueryVoPage);
    }

    /**
     * 获取对话历史的工具调用
     */
    @GetMapping("toolLog")
    public BaseResponse<List<AiToolLogVo>> toolLog(Long messageId) {
        if (messageId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
       List <AiToolLogVo> aiToolLogVos = aiToolLogService.selectToolLogs(messageId);
        if (aiToolLogVos == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
       return ResultUtils.success(aiToolLogVos);
    }


}
