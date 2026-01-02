package com.zriyo.aicodemother.core.saver;

import com.zriyo.aicodemother.ai.model.HtmlCodeResult;
import com.zriyo.aicodemother.ai.model.MultiFileCodeResult;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {
    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaverTemplate = new HtmlCodeFileSaverTemplate();
    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaverTemplate = new MultiFileCodeFileSaverTemplate();
    public static File saveCode(Object code, CodeGenTypeEnum codeGenTypeEnum,Long appId) {
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeFileSaverTemplate.saveCode((HtmlCodeResult) code,appId);
            case MULTI_FILE -> multiFileCodeFileSaverTemplate.saveCode((MultiFileCodeResult) code,appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的生成类型");
        };
    }
}
