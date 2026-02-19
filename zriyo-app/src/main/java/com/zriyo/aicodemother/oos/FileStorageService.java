package com.zriyo.aicodemother.oos;

import com.zriyo.aicodemother.config.S3Config;
import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

/**
 * 文件存储服务
 * 基于 AWS S3 协议的对象存储
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Slf4j
@Service
@ConditionalOnBean(S3Client.class)
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

    /**
     * 上传文件到指定 bucket (UUID 方案)
     *
     * @param bucketLogicalName 逻辑 bucket 名称
     * @param file              要上传的文件
     * @return 访问 URL
     */
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

            log.info("文件上传成功 | bucket={}, key={}", actualBucketName, key);

            return buildAccessUrl(bucketSpec, actualBucketName, key);

        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }

    /**
     * 根据查询参数获取或上传文件 (用于图片代理缓存)
     * 如果文件已存在则返回 URL，否则上传新文件
     *
     * @param bucketLogicalName 逻辑 bucket 名称
     * @param query             查询参数 (用于生成稳定的 key)
     * @param file              要上传的文件
     * @return 访问 URL
     */
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

        // 先查询文件是否存在
        if (exists(actualBucketName, key)) {
            log.info("命中本地路径缓存 | bucket={}, key={}", actualBucketName, key);
            return buildAccessUrl(bucketSpec, actualBucketName, key);
        }

        // 不存在则上传
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(actualBucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            log.info("新回源文件入桶 | bucket={}, key={}", actualBucketName, key);
            return buildAccessUrl(bucketSpec, actualBucketName, key);

        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }

    /**
     * 根据查询参数获取文件 URL (不触发上传)
     *
     * @param bucketLogicalName 逻辑 bucket 名称
     * @param query             查询参数
     * @return 访问 URL，如果文件不存在则返回 null
     */
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

    /**
     * 生成稳定的本地存储路径 key
     * 去掉 MD5 和日期目录，方便管理和快速查询
     *
     * @param query 查询参数
     * @return 存储路径 key
     */
    private String getSafeKeyByQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "query 不能为空");
        }
        // 直接用关键词映射路径，方便管理和秒查
        // proxy_cache/关键词.jpg
        String safeName = query.trim().toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "_");
        return "proxy_cache/" + safeName + ".jpg";
    }

    /**
     * 检查文件是否存在
     *
     * @param bucket bucket 名称
     * @param key    文件 key
     * @return 是否存在
     */
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

    /**
     * 构建访问 URL
     * 公共 bucket 直接返回 URL，私有 bucket 返回预签名 URL
     *
     * @param spec   bucket 配置
     * @param bucket bucket 名称
     * @param key    文件 key
     * @return 访问 URL
     */
    private String buildAccessUrl(S3Config.BucketSpec spec, String bucket, String key) {
        if (S3Config.BucketSpec.BucketType.PUBLIC.equals(spec.getType())) {
            return buildPathStyleUrl(bucket, key);
        }
        return generatePresignedUrl(bucket, key, Duration.ofHours(1));
    }

    /**
     * 构建 Path-Style URL
     *
     * @param bucket bucket 名称
     * @param key    文件 key
     * @return URL
     */
    private String buildPathStyleUrl(String bucket, String key) {
        String baseUrl = s3Config.getEndpoint();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + bucket + "/" + key;
    }

    /**
     * 生成预签名 URL (用于私有 bucket)
     *
     * @param bucket     bucket 名称
     * @param key        文件 key
     * @param expiration 有效期
     * @return 预签名 URL
     */
    private String generatePresignedUrl(String bucket, String key, Duration expiration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(req -> req.bucket(bucket).key(key))
                .build();

        PresignedGetObjectRequest presignedRequest =
                s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

    /**
     * 校验文件
     *
     * @param file 要校验的文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "文件大小不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB"
            );
        }
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件类型: " + contentType);
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
