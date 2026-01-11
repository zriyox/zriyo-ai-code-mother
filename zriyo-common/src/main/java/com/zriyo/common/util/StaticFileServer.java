package com.zriyo.common.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class StaticFileServer {

    private static final ConcurrentHashMap<String, HttpServer> serverRegistry = new ConcurrentHashMap<>();

    private static final ThreadPoolExecutor globalExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2)
    );

    public static String start(String rootDir) throws IOException {
        Path rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
        if (!Files.exists(rootPath)) throw new IllegalArgumentException("目录不存在: " + rootPath);

        // 绑定 127.0.0.1，Playwright 容器内访问更稳定
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int actualPort = server.getAddress().getPort();

        server.createContext("/", new StaticHandler(rootPath));
        server.setExecutor(globalExecutor);
        server.start();

        String url = "http://127.0.0.1:" + actualPort + "/";
        serverRegistry.put(url, server);

        log.info("🚀 静态服务器已就绪 (并发模式): {} -> {}", url, rootPath);
        return url;
    }

    public static void stop(String url) {
        HttpServer server = serverRegistry.remove(url);
        if (server != null) {
            server.stop(0);
            log.info("⏹️ 服务器资源已释放: {}", url);
        }
    }

    public static class StaticHandler implements HttpHandler {
        private final Path root;

        public StaticHandler(Path root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) {
            String requestPath = exchange.getRequestURI().getPath();
            try {
                Path filePath = resolveRequestedFile(requestPath);

                // 1. SPA 支持
                if (!Files.exists(filePath) && !requestPath.contains(".")) {
                    filePath = root.resolve("index.html");
                }

                // 2. 404 判定
                if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                    sendResponse(exchange, 404, "Not Found");
                    return;
                }

                // 3. 安全校验
                if (!filePath.startsWith(root)) {
                    sendResponse(exchange, 403, "Forbidden");
                    return;
                }

                // 4. 读取内容
                byte[] content = Files.readAllBytes(filePath);

                // ✅ 关键修复：设置 Content-Type 及其它必要 Header
                String contentType = getContentType(filePath.toString());
                exchange.getResponseHeaders().set("Content-Type", contentType);

                // 解决跨域限制，确保 Playwright 能够无障碍抓取资源
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

                // 禁用缓存，防止 AI 诊断时拿到旧的编译产物
                exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
                exchange.getResponseHeaders().set("Pragma", "no-cache");
                exchange.getResponseHeaders().set("Expires", "0");

                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } catch (Exception e) {
                log.error("静态请求异常: {}", requestPath, e);
                try { sendResponse(exchange, 500, "Server Error"); } catch (IOException ignored) {}
            }
        }

        private Path resolveRequestedFile(String requestPath) {
            String path = requestPath.equals("/") ? "index.html" : requestPath;
            if (path.startsWith("/")) path = path.substring(1);
            return root.resolve(path).normalize();
        }

        private void sendResponse(HttpExchange exchange, int code, String msg) throws IOException {
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }

        // ✅ 核心 MIME 映射函数：解决 import.meta 报错的关键
        private String getContentType(String filename) {
            String lower = filename.toLowerCase();

            // Vite 构建产物必须以正确的 javascript 类型返回，否则无法解析 import.meta
            if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
                return "application/javascript; charset=utf-8";
            }
            if (lower.endsWith(".html")) {
                return "text/html; charset=utf-8";
            }
            if (lower.endsWith(".css")) {
                return "text/css; charset=utf-8";
            }
            if (lower.endsWith(".json")) return "application/json";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".ico")) return "image/x-icon";

            return "application/octet-stream";
        }
    }
}
