package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.model.entity.DeploymentHistory;
import com.zriyo.aicodemother.service.DeploymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部署历史表控制器
 */
@RestController
@Validated
@RequestMapping("/deployment/history")
@RequiredArgsConstructor
public class DeploymentHistoryController {
    private final DeploymentHistoryService deploymentHistoryService;

    /**
     * 游标翻页
     * @param id 初始 0 每次查 15个数据 下次携带 翻页的最大 Id 请求
     */
    @GetMapping("/list/{id}")
    public ResponseEntity<List<DeploymentHistory>> getDeploymentHistory(@PathVariable Long id) {
        return ResponseEntity.ok(deploymentHistoryService.getDeploymentHistoryWithCursorPagination(id));
    }



}
