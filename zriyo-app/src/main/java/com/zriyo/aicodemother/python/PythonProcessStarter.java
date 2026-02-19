package com.zriyo.aicodemother.python;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Python 进程启动器
 * 在 Java 应用启动时自动启动 Python 服务，关闭时自动停止
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Slf4j
@Component
@Order(1)
public class PythonProcessStarter implements ApplicationRunner {

    @Value("${python.port:8000}")
    private int pythonPort;

    @Value("${python.path:../python-ai-server}")
    private String pythonPath;

    @Value("${python.enabled:true}")
    private boolean pythonEnabled;

    @Value("${python.auto-start:true}")
    private boolean autoStart;

    @Value("${python.reload:true}")
    private boolean pythonReload;

    private Process pythonProcess;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!pythonEnabled || !autoStart) {
            log.info("Python 自动启动已禁用 (enabled={}, auto-start={})", pythonEnabled, autoStart);
            return;
        }

        if (isPythonRunning()) {
            log.info("Python 服务已在运行，跳过启动");
            return;
        }

        startPythonProcess();
        waitForPythonReady();
    }

    private void startPythonProcess() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        Path projectPath = Paths.get(pythonPath).toAbsolutePath();

        if (!Files.exists(projectPath)) {
            log.warn("Python 项目路径不存在: {}，跳过启动", projectPath);
            return;
        }

        log.info("Python 项目路径: {}, 热重载: {}", projectPath, pythonReload);

        ProcessBuilder pb;
        String reloadFlag = pythonReload ? " --reload" : "";

        if (os.contains("win")) {
            String venvPython = projectPath.resolve("venv/Scripts/python.exe").toString();
            pb = new ProcessBuilder(
                venvPython,
                "-m", "uvicorn", "app.main:app",
                "--host", "127.0.0.1",
                "--port", String.valueOf(pythonPort),
                "--reload"  // Windows: 直接加参数
            );
        } else {
            pb = new ProcessBuilder(
                "bash", "-c",
                "cd " + projectPath + " && source venv/bin/activate && " +
                "uvicorn app.main:app --host 127.0.0.1 --port " + pythonPort +
                reloadFlag  // Unix: 根据配置添加
            );
        }

        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);

        pythonProcess = pb.start();

        new Thread(this::readProcessOutput, "Python-Output-Reader").start();

        log.info("Python 进程已启动，PID: {}", pythonProcess.pid());
        log.info("等待 Python 服务启动在 http://127.0.0.1:{}", pythonPort);
    }

    private void readProcessOutput() {
        try {
            pythonProcess.inputReader().lines().forEach(line -> {
                if (log.isDebugEnabled()) {
                    log.debug("[Python] {}", line);
                }
            });
        } catch (Exception e) {
            if (pythonProcess.isAlive()) {
                log.warn("读取 Python 进程输出异常: {}", e.getMessage());
            }
        }
    }

    private void waitForPythonReady() {
        int maxRetries = 30;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(1000);
                if (isPythonRunning()) {
                    log.info("✓ Python 服务就绪");
                    return;
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Python 服务启动超时，但继续运行");
    }

    private boolean isPythonRunning() {
        try {
            java.net.Socket socket = new java.net.Socket("127.0.0.1", pythonPort);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @jakarta.annotation.PreDestroy
    public void stopPython() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            log.info("正在停止 Python 进程...");
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                pythonProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            log.info("Python 进程已停止");
        }
    }

    public boolean isPythonProcessAlive() {
        return pythonProcess != null && pythonProcess.isAlive();
    }

    public int getPythonPort() {
        return pythonPort;
    }
}
