package com.hechang.codeagent.controller;

import com.hechang.codeagent.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 静态资源访问
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    private static final Path PREVIEW_ROOT_PATH = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();

    private static final Path DEPLOY_ROOT_PATH = Path.of(AppConstant.CODE_DEPLOY_ROOT_DIR).toAbsolutePath().normalize();

    /**
     * 提供生成代码预览访问，支持目录重定向。
     * 访问格式：http://localhost:8125/api/static/{previewKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        return serveResource(PREVIEW_ROOT_PATH, deployKey, "/static/" + deployKey, request, false);
    }

    /**
     * 访问已部署的应用。部署文件与生成代码预览文件位于不同目录，不能共用上面的预览入口。
     */
    @GetMapping("/deployments/{deployKey}/**")
    public ResponseEntity<Resource> serveDeploymentResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        return serveResource(DEPLOY_ROOT_PATH, deployKey, "/static/deployments/" + deployKey, request, true);
    }

    private ResponseEntity<Resource> serveResource(
            Path rootPath,
            String resourceKey,
            String mappingPrefix,
            HttpServletRequest request,
            boolean spaFallback) {
        if (!resourceKey.matches("[A-Za-z0-9_-]+")) {
            return ResponseEntity.notFound().build();
        }
        String pathWithinMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (pathWithinMapping == null || !pathWithinMapping.startsWith(mappingPrefix)) {
            return ResponseEntity.notFound().build();
        }
        String resourcePath = pathWithinMapping.substring(mappingPrefix.length());
        if (resourcePath.isEmpty()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(request.getRequestURI() + "/"));
            return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
        }
        if (resourcePath.equals("/")) {
            resourcePath = "/index.html";
        }

        Path resourceRoot = rootPath.resolve(resourceKey).normalize();
        Path targetPath = resourceRoot.resolve(resourcePath.substring(1)).normalize();
        if (!targetPath.startsWith(resourceRoot)) {
            return ResponseEntity.notFound().build();
        }
        if (!Files.isRegularFile(targetPath) && spaFallback && !resourcePath.substring(1).contains(".")) {
            targetPath = resourceRoot.resolve("index.html");
        }
        if (!Files.isRegularFile(targetPath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(targetPath);
        return ResponseEntity.ok()
                // Windows 通常将 .js 识别为 text/plain；Vite 的模块脚本必须使用 JavaScript MIME 类型。
                .header(HttpHeaders.CONTENT_TYPE, getContentTypeWithCharset(targetPath.toString()))
                .body(resource);
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        if (filePath.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
