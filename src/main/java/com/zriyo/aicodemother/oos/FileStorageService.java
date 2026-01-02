package com.zriyo.aicodemother.oos;

import com.zriyo.aicodemother.config.S3Config;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(
            Arrays.asList("image/jpeg", "image/png", "image/webp")
    );

    public FileStorageService(S3Client s3Client,
                              S3Presigner s3Presigner,
                              S3Config s3Config) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.s3Config = s3Config;
    }

    // =========================================================
    // 你原来的方法：完全保留（UUID 方案）
    // =========================================================
    public String uploadFile(String bucketLogicalName, MultipartFile file) {
        validateFile(file);

        S3Config.BucketSpec bucketSpec = s3Config.getBuckets().get(bucketLogicalName);
        if (bucketSpec == null) {
            throw new RuntimeException("未配置的存储桶: " + bucketLogicalName);
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String fileExtension = getFileExtension(originalFilename);

        String dateDir = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String key = dateDir + "/"
                + UUID.randomUUID()
                + (StringUtils.hasText(fileExtension) ? "." + fileExtension : "");

        String actualBucketName = bucketSpec.getName();

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(actualBucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            log.info("✅ 文件上传成功 | bucket={}, key={}", actualBucketName, key);

            return buildAccessUrl(bucketSpec, actualBucketName, key);

        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }

    // =========================================================
    // ✅ 保持原有 getOrUploadByQuery (适配你的 ImageProxy 逻辑)
    // =========================================================
    public String getOrUploadByQuery(String bucketLogicalName,
                                     String query,
                                     MultipartFile file) {

        validateFile(file);

        S3Config.BucketSpec bucketSpec = s3Config.getBuckets().get(bucketLogicalName);
        if (bucketSpec == null) {
            throw new RuntimeException("未配置的存储桶: " + bucketLogicalName);
        }

        String key = getSafeKeyByQuery(query);
        String actualBucketName = bucketSpec.getName();

        // 1️⃣ 先查
        if (exists(actualBucketName, key)) {
            log.info("🎯 命中本地路径缓存 | bucket={}, key={}", actualBucketName, key);
            return buildAccessUrl(bucketSpec, actualBucketName, key);
        }

        // 2️⃣ 不存在才上传
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(actualBucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            log.info("⬆️ 新回源文件入桶 | bucket={}, key={}", actualBucketName, key);
            return buildAccessUrl(bucketSpec, actualBucketName, key);

        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }

    // =========================================================
    // ✅ 追加方法：仅查询 URL 不触发下载
    // =========================================================
    public String getFileUrlByQuery(String bucketLogicalName, String query) {
        S3Config.BucketSpec bucketSpec = s3Config.getBuckets().get(bucketLogicalName);
        if (bucketSpec == null) return null;

        String key = getSafeKeyByQuery(query);
        String actualBucketName = bucketSpec.getName();

        if (exists(actualBucketName, key)) {
            return buildAccessUrl(bucketSpec, actualBucketName, key);
        }
        return null;
    }

    // =========================================================
    // ✅ 追加辅助方法：生成确定的本地桶路径 (去掉 MD5 和日期目录)
    // =========================================================
    private String getSafeKeyByQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "query 不能为空");
        }
        // 直接用关键词映射路径，方便管理和秒查
        // proxy_cache/关键词.jpg
        String safeName = query.trim().toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "_");
        return "proxy_cache/" + safeName + ".jpg";
    }

    // =========================================================
    // MD5 相关（你原来的，保留不动）
    // =========================================================
    private String md5OfQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "query 不能为空");
        }

        String normalized = query
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");

        return md5(normalized);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    // =========================================================
    // S3 辅助方法（你原来的，保留不动）
    // =========================================================
    private boolean exists(String bucket, String key) {
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildAccessUrl(S3Config.BucketSpec spec, String bucket, String key) {
        if (S3Config.BucketSpec.BucketType.PUBLIC.equals(spec.getType())) {
            return buildPathStyleUrl(bucket, key);
        }
        return generatePresignedUrl(bucket, key, Duration.ofHours(1));
    }

    private String buildPathStyleUrl(String bucket, String key) {
        String baseUrl = s3Config.getEndpoint();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + bucket + "/" + key;
    }

    private String generatePresignedUrl(String bucket, String key, Duration expiration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(req -> req.bucket(bucket).key(key))
                .build();

        PresignedGetObjectRequest presignedRequest =
                s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

    // =========================================================
    // 你原来的校验与工具方法：完全保留
    // =========================================================
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "文件大小不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB"
            );
        }
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型: " + contentType);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
