package com.hechang.codeagent.constant;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 应用常量
 */
public interface AppConstant {

    /**
     * 生成、部署和截图文件的统一存储根目录。
     * 优先使用 code.ai.storage.root 系统属性或 CODE_AI_STORAGE_ROOT 环境变量；
     * 本地多模块开发时自动定位到包含 code-ai-app 的项目根目录，避免不同启动目录产生不同 tmp 路径。
     */
    String CODE_STORAGE_ROOT_DIR = resolveStorageRootDir();

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = CODE_STORAGE_ROOT_DIR + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = CODE_STORAGE_ROOT_DIR + "/tmp/code_deploy";

    static String resolveStorageRootDir() {
        String configuredRoot = System.getProperty("code.ai.storage.root");
        if (configuredRoot == null || configuredRoot.isBlank()) {
            configuredRoot = System.getenv("CODE_AI_STORAGE_ROOT");
        }
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot).toAbsolutePath().normalize().toString();
        }

        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.isDirectory(userDir.resolve("code-ai-app"))) {
            return userDir.toString();
        }
        Path parentDir = userDir.getParent();
        if (parentDir != null && Files.isDirectory(parentDir.resolve("code-ai-app"))) {
            return parentDir.toString();
        }
        return userDir.toString();
    }

}
