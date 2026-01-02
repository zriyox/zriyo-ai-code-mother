package com.zriyo.aicodemother.service;

import com.zriyo.aicodemother.oos.FileStorageService;
import com.zriyo.aicodemother.util.SpringUtils;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProxyService {

    private final FileStorageService fileStorageService;

    // 默认占位图：建议放在 OOS 里的一个固定位置
    private static final String DEFAULT_PLACEHOLDER_URL = "https://io.zriyo.com/zriyo-code/default-placeholder.jpg";

    @Value("${sogou.api.url}")
    private String SOGOU_API_URL;

    @Value("${sogou.api.id}")
    private String API_ID;

    @Value("${sogou.api.key}")
    private String API_KEY;

    private RestTemplate restTemplate() {
        return SpringUtils.getBean(RestTemplate.class);
    }

    /**
     * ✅ 终极逻辑：Keyword 直连 + 异常降级
     */
    public String getAndUploadImage(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return DEFAULT_PLACEHOLDER_URL;
        }

        // 1️⃣ 第一步：清理 Keyword 防止文件名非法
        String safeKeyword = keyword.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");

        try {
            // 2️⃣ 第二步：本地桶直接路径查询（最快，只发 HEAD 请求）
            // 注意：这里调用的是新加的查询方法，不再触发下载逻辑
            String existingUrl = fileStorageService.getFileUrlByQuery("appImage", safeKeyword);
            if (StringUtils.hasText(existingUrl)) {
                log.debug("🎯 OOS 路径直接命中: {}", safeKeyword);
                return existingUrl;
            }

            // 3️⃣ 第三步：本地无缓存，回源抓取
            log.info("🌐 OOS 未命中，回源抓取: {}", safeKeyword);
            MultipartFile imageFile = downloadFromSogou(safeKeyword);

            // 4️⃣ 第四步：上传至 OOS 并返回最终访问 URL
            return fileStorageService.getOrUploadByQuery(
                    "appImage",
                    safeKeyword,
                    imageFile
            );

        } catch (Exception e) {
            // ❌ 异常降级：不管是 API 挂了、网络超时还是 S3 异常，统一返回占位图
            log.error("❌ 图片获取链路异常 [keyword={}], 触发兜底降级. 原因: {}", safeKeyword, e.getMessage());
            return DEFAULT_PLACEHOLDER_URL;
        }
    }

    /**
     * 从搜狗回源下载二进制内容并封装为 MultipartFile
     */
    private MultipartFile downloadFromSogou(String keyword) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = String.format("%s?id=%s&key=%s&words=%s&page=1&type=1",
                SOGOU_API_URL, API_ID, API_KEY, encodedKeyword);

        // 1. 获取图片地址列表
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

        // 2. 模拟浏览器 User-Agent 下载图片
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        ResponseEntity<byte[]> resp = restTemplate().exchange(
                targetUrl, HttpMethod.GET, new HttpEntity<>(headers), byte[].class
        );

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("源站文件下载失败, HTTP Status: " + resp.getStatusCode());
        }

        // 3. 获取 ContentType，默认 image/jpeg
        String contentType = resp.getHeaders().getContentType() != null
                ? resp.getHeaders().getContentType().toString() : "image/jpeg";

        return new ByteArrayMultipartFile(resp.getBody(), keyword + ".jpg", contentType);
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

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
