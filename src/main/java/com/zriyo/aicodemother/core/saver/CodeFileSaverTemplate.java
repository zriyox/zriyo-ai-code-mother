package com.zriyo.aicodemother.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public abstract class CodeFileSaverTemplate<T> {
    private final String FILE_SAVE_ROOT_DIR = com.zriyo.aicodemother.util.CodeOutputManager.getCodeOutputBaseDir().toString();

    public final File saveCode(T result,Long appId) {
        validateInput(result);
        //构建唯一目录
        String filePath = buildUniqueDir(appId);
        //保存文件交给子类实现
        saveCodeToFile(filePath, result);
        return new File(filePath);
    }

    /**
     * 构建文件的唯一路径：tmp/code_output/bizType_雪花 ID
     *
     */
    private String buildUniqueDir() {
        String value = this.getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", value, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }
    /**
     * 构建文件的唯一路径：tmp/code_output/bizType_AppId
     *
     */
    private String buildUniqueDir(Long appId) {
        if (appId == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "appId不能为空");
        }
        String value = this.getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", value, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param result 代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 保存单个文件
     *
     * @param dirPath
     * @param filename
     * @param content
     */
    public  void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }


    public abstract CodeGenTypeEnum getCodeType();

    public abstract void saveCodeToFile(String dirPath, T result);
}
