package com.hechang.codeagent.core.builder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 构建 Vue 项目
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 异步构建 Vue 项目
     *
     * @param projectPath
     */
    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis())
                .start(() -> {
                    try {
                        buildProject(projectPath);
                    } catch (Exception e) {
                        log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在：{}", projectPath);
            return false;
        }
        // 检查是否有 package.json 文件
        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()) {
            log.error("项目目录中没有 package.json 文件：{}", projectPath);
            return false;
        }
        log.info("开始构建 Vue 项目：{}", projectPath);
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败：{}", projectPath);
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败：{}", projectPath);
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            log.error("构建完成但 dist 目录未生成：{}", projectPath);
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录：{}", projectPath);
        return true;
    }

    /**
     * 执行 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");

        // 核心修改：
        // 1. --registry 淘宝源加速
        // 2. --ignore-scripts 跳过 prepare/postinstall 等容易在自动化环境中报错的生命周期脚本
        // 3. --no-audit --no-fund 关闭安全审查和资金提示，让控制台输出更干净，提升速度
        String[] command = buildCommandArray(
                "install",
                "--registry=https://registry.npmmirror.com",
                "--ignore-scripts",
                "--no-audit",
                "--no-fund"
        );
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String[] command = buildCommandArray("run", "build");
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }

    /**
     * 构造跨平台的命令数组
     */
    private String[] buildCommandArray(String... args) {
        List<String> commandList = new ArrayList<>();
        if (isWindows()) {
            commandList.add("cmd.exe");
            commandList.add("/d"); // 核心修改：禁用注册表 AutoRun 脚本，防止环境污染
            commandList.add("/c");
            commandList.add("npm");
        } else {
            commandList.add("npm");
        }
        commandList.addAll(Arrays.asList(args));
        return commandList.toArray(new String[0]);
    }

    /**
     * 根据操作系统构造命令
     *
     * @param baseCommand
     * @return
     */
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * 操作系统检测
     *
     * @return
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 执行命令
     */
    private boolean executeCommand(File workingDir, String[] command, int timeoutSeconds) {
        String commandStr = String.join(" ", command);
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), commandStr);

            // 使用 String[] 而不是 String，避免路径或参数中有空格被错误分割
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(true); // 合并错误流到标准输出

            Process process = processBuilder.start();

            // 异步读取日志并解决 Windows GBK 乱码
            Thread.ofVirtual().name("cmd-reader-" + System.currentTimeMillis()).start(() -> {
                // Windows cmd 的默认编码是 GBK，处理中文乱码
                Charset charset = isWindows() ? Charset.forName("GBK") : Charset.defaultCharset();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), charset))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[NPM Output] {}", line);
                    }
                } catch (Exception e) {
                    log.error("读取命令输出流失败: {}", e.getMessage());
                }
            });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", commandStr);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", commandStr, e.getMessage(), e);
            return false;
        }
    }
}
