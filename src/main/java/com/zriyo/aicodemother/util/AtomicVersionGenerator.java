package com.zriyo.aicodemother.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 线程安全的语义化版本自增器（单机内存版）
 * 初始版本: v1.0.0
 * 默认每次递增 patch 版本（v1.0.0 → v1.0.1）
 */
public class AtomicVersionGenerator {

    //初始版本号
    public static final String INITIAL_VERSION = "v1.0.0";
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)$");

    // 使用 AtomicReference 保证引用切换的原子性
    private final AtomicReference<Version> currentVersion;

    public AtomicVersionGenerator() {
        this("v1.0.0");
    }

    public AtomicVersionGenerator(String initialVersion) {
        this.currentVersion = new AtomicReference<>(parseVersion(initialVersion));
    }

    /**
     * 线程安全地获取并递增到下一个版本（patch +1）
     * @return 新版本字符串，如 "v1.0.1"
     */
    public String nextVersion() {
        Version oldVer;
        Version newVer;
        do {
            oldVer = currentVersion.get();
            newVer = oldVer.incrementPatch();
        } while (!currentVersion.compareAndSet(oldVer, newVer));
        return newVer.toString();
    }

    /**
     * 获取当前版本（不递增）
     */
    public String currentVersion() {
        return currentVersion.get().toString();
    }

    // ----- 内部版本类（不可变） -----
    private static class Version {
        final int major;
        final int minor;
        final int patch;

        Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public Version incrementPatch() {
            return new Version(major, minor, patch + 1);
        }

        public Version incrementMinor() {
            return new Version(major, minor + 1, 0);
        }

        public Version incrementMajor() {
            return new Version(major + 1, 0, 0);
        }

        @Override
        public String toString() {
            return "v" + major + "." + minor + "." + patch;
        }
    }

    public static Version parseVersion(String versionStr) {
        if (versionStr == null || versionStr.trim().isEmpty()) {
            throw new IllegalArgumentException("版本不能为空");
        }
        String clean = versionStr.startsWith("v") ? versionStr.substring(1) : versionStr;
        var matcher = SEMVER_PATTERN.matcher("v" + clean);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("版本格式错误: " + versionStr + "，应为 v1.2.0");
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        return new Version(major, minor, patch);
    }

    // ===== 测试用例 =====
    public static void main(String[] args) throws InterruptedException {
        AtomicVersionGenerator generator = new AtomicVersionGenerator("v1.2.0");

        // 多线程测试
        Runnable task = () -> {
            for (int i = 0; i < 3; i++) {
                System.out.println(Thread.currentThread().getName() + ": " + generator.nextVersion());
            }
        };

        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("最终版本: " + generator.currentVersion());
        // 输出示例（顺序可能不同，但无重复、连续）：
        // T1: v1.2.1
        // T2: v1.2.2
        // T1: v1.2.3
        // T2: v1.2.4
        // T1: v1.2.5
        // T2: v1.2.6
        // 最终版本: v1.2.6
    }
}
