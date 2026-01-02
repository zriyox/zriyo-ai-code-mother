// com.zriyo.aicodemother.controller.SController.java
package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.oos.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传统一入口控制器
 * - /s/avatar   → 上传公开头像
 * - /s/document → 上传私有文档
 */
@Slf4j
@RestController
@RequestMapping("/s")
@RequiredArgsConstructor
public class SController {

    private final FileStorageService fileStorageService;

    /**
     * 上传用户头像（公开访问）
     * POST /s/avatar
     * Form-data: file=xxx.png
     */
    @PostMapping("/avatar")
    public BaseResponse<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.uploadFile("avatars", file);
            return ResultUtils.success(url);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e){
            log.error(" 文件上传失败:{}", e.getMessage());
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }
    /**
     * 通用上传（可选，默认走私有）
     * POST /s/upload
     */
    @PostMapping("/upload")
    public BaseResponse<?> uploadGeneric(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.uploadFile("avatars", file);
            return ResultUtils.success(url);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e){
            log.error(" 文件上传失败:{}", e.getMessage());
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }
}
