package com.zriyo.aicodemother.core.saver;

import com.zriyo.aicodemother.ai.model.MultiFileCodeResult;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;

public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {
    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    public void saveCodeToFile(String dirPath, MultiFileCodeResult result) {
        writeToFile(dirPath, com.zriyo.aicodemother.model.AppConstant.STATIC_ENTRY_FILE, result.getHtmlCode());
        writeToFile(dirPath, "style.css", result.getCssCode());
        writeToFile(dirPath, "script.js", result.getJsCode());
    }
}
