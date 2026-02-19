package com.zriyo.aicodemother.service;

import com.zriyo.aicodemother.oos.FileStorageService;
import com.zriyo.common.util.SpringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 图片代理服务
 * 用于从外部 API 获取图片并缓存到 OOS
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProxyService {

    private final FileStorageService fileStorageService;

    /**
     * 默认占位图
     */
    private static final String DEFAULT_PLACEHOLDER_URL = "https://io.zriyo.com/zriyo-code/default-placeholder.jpg";

    @Value("${sogou.api.url:}")
    private String SOGOU_API_URL;

    @Value("${sogou.api.id:}")
    private String API_ID;

    @Value("${sogou.api.key:}")
    private String API_KEY;

    /**
     * 获取图片并上传到 OOS
     * 逻辑：
     * 1. 先检查 OOS 缓存
     * 2. 如果缓存命中，直接返回 URL
     * 3. 如果缓存未命中，从搜狗 API 下载图片
     * 4. 上传到 OOS 并返回 URL
     * 5. 任何异常情况返回默认占位图
     *
     * @param keyword 图片关键词
     * @return 图片 URL
     */
    public String getAndUploadImage(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return DEFAULT_PLACEHOLDER_URL;
        }

        // 清理 Keyword 防止文件名非法
        String safeKeyword = keyword.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");

        try {
            // 本地桶直接路径查询（最快，只发 HEAD 请求）
            String existingUrl = fileStorageService.getFileUrlByQuery("appImage", safeKeyword);
            if (StringUtils.hasText(existingUrl)) {
                log.debug("OOS 路径直接命中: {}", safeKeyword);
                return existingUrl;
            }

            // 本地无缓存，回源抓取
            log.info("OOS 未命中，回源抓取: {}", safeKeyword);
            MultipartFile imageFile = downloadFromSogou(safeKeyword);

            // 上传至 OOS 并返回最终访问 URL
            return fileStorageService.getOrUploadByQuery(
                    "appImage",
                    safeKeyword,
                    imageFile
            );

        } catch (Exception e) {
            // 异常降级：返回默认占位图
            log.error("图片获取链路异常 [keyword={}], 触发兜底降级. 原因: {}", safeKeyword, e.getMessage());
            return DEFAULT_PLACEHOLDER_URL;
        }
    }

    /**
     * 从搜狗 API 下载图片
     *
     * @param keyword 图片关键词
     * @return MultipartFile
     */
    private MultipartFile downloadFromSogou(String keyword) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = String.format("%s?id=%s&key=%s&words=%s&page=1&type=1",
                SOGOU_API_URL, API_ID, API_KEY, encodedKeyword);

        // 获取图片地址列表
        Map<?, ?> response = restTemplate().getForObject(url, Map.class);
        if (response == null || !Integer.valueOf(200).equals(response.get("code"))) {
            throw new RuntimeException("Sogou API 接口请求异常");
        }

        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) response.get("res");
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new RuntimeException("搜狗 API 未返回图片结果");
        }

        String targetUrl = imageUrls.get(0);

        // 模拟浏览器 User-Agent 下载图片
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        ResponseEntity<byte[]> resp = restTemplate().exchange(
                targetUrl, HttpMethod.GET, new HttpEntity<>(headers), byte[].class
        );

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("源站文件下载失败, HTTP Status: " + resp.getStatusCode());
        }

        // 获取 ContentType，默认 image/jpeg
        String contentType = resp.getHeaders().getContentType() != null
                ? resp.getHeaders().getContentType().toString() : "image/jpeg";

        return new ByteArrayMultipartFile(resp.getBody(), keyword + ".jpg", contentType);
    }

    /**
     * 获取 RestTemplate Bean
     *
     * @return RestTemplate
     */
    private RestTemplate restTemplate() {
        return SpringUtils.getBean(RestTemplate.class);
    }

    /**
     * 内存 MultipartFile 实现类
     */
    static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;
        private final String contentType;

        ByteArrayMultipartFile(byte[] content, String filename, String contentType) {
            this.content = content;
            this.filename = filename;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
