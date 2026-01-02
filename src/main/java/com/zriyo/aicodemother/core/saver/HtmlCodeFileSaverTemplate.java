package com.zriyo.aicodemother.core.saver;

import com.zriyo.aicodemother.ai.model.HtmlCodeResult;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;

public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult>{
    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        if (result.getHtmlCode() == null || result.getHtmlCode().isEmpty()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "HTML代码不能为空");
        }
    }

    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    public void saveCodeToFile(String dirPath, HtmlCodeResult result) {
        writeToFile(dirPath, com.zriyo.aicodemother.model.AppConstant.STATIC_ENTRY_FILE, result.getHtmlCode());
    }
}
