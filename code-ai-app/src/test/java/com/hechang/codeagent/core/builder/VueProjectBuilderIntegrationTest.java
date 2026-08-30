package com.hechang.codeagent.core.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 Vue 工程模式的本地构建链路。
 *
 * <p>测试使用真实的 npm 与 Vite，但在临时目录创建最小工程，
 * 不依赖 AI、数据库或 Nacos。这样 {@link VueProjectBuilder} 的命令、
 * 依赖安装和生产构建任一环节失效时，测试都会明确失败。</p>
 */
class VueProjectBuilderIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void buildsVueProjectAndProducesBrowserEntryAndJavaScriptBundle() throws IOException {
        Path projectDirectory = temporaryDirectory.resolve("vue-project");
        writeVueViteProject(projectDirectory);

        boolean built = new VueProjectBuilder().buildProject(projectDirectory.toString());

        Path distDirectory = projectDirectory.resolve("dist");
        Path browserEntry = distDirectory.resolve("index.html");
        assertTrue(built, "VueProjectBuilder 应返回构建成功");
        assertTrue(Files.isRegularFile(browserEntry), "生产构建必须生成浏览器入口 dist/index.html");
        assertTrue(Files.readString(browserEntry).contains("assets/"), "入口 HTML 必须引用 Vite 打包后的静态资源");
        try (Stream<Path> outputFiles = Files.walk(distDirectory)) {
            assertTrue(outputFiles.anyMatch(path -> path.getFileName().toString().endsWith(".js")),
                    "生产构建必须生成 JavaScript bundle，浏览器才能渲染 Vue 页面");
        }
    }

    private void writeVueViteProject(Path projectDirectory) throws IOException {
        Files.createDirectories(projectDirectory.resolve("src"));
        Files.writeString(projectDirectory.resolve("package.json"), """
                {
                  "name": "vue-project-builder-integration-test",
                  "private": true,
                  "version": "1.0.0",
                  "scripts": { "build": "vite build" },
                  "dependencies": { "vue": "^3.5.0" },
                  "devDependencies": {
                    "@vitejs/plugin-vue": "^5.2.0",
                    "vite": "^5.4.0"
                  }
                }
                """);
        Files.writeString(projectDirectory.resolve("index.html"), """
                <!doctype html>
                <html lang="zh-CN">
                  <head><meta charset="UTF-8" /><title>Vue Builder Test</title></head>
                  <body><div id="app"></div><script type="module" src="/src/main.js"></script></body>
                </html>
                """);
        Files.writeString(projectDirectory.resolve("vite.config.js"), """
                import { defineConfig } from 'vite'
                import vue from '@vitejs/plugin-vue'

                export default defineConfig({ base: './', plugins: [vue()] })
                """);
        Files.writeString(projectDirectory.resolve("src/main.js"), """
                import { createApp } from 'vue'
                import App from './App.vue'

                createApp(App).mount('#app')
                """);
        Files.writeString(projectDirectory.resolve("src/App.vue"), """
                <template>
                  <main><h1>Vue Builder E2E</h1><p>页面可被 Vite 生产构建。</p></main>
                </template>
                """);
    }
}
