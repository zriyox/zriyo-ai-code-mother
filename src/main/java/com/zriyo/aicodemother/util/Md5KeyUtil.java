package com.zriyo.aicodemother.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Md5KeyUtil {

    private Md5KeyUtil() {}

    /**
     * 对查询内容做归一化 + MD5
     * 这是整个对象存储“寻址一致性”的核心
     */
    public static String md5OfQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query 不能为空");
        }

        String normalized = query
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");

        return md5(normalized);
    }

    private static String md5(String input) {
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
}
