package com.hechang.codeagent.controller;

import com.hechang.codeagent.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaticResourceControllerTest {

    private static final String PREVIEW_KEY = "mime-type-regression";

    @AfterEach
    void cleanUp() throws IOException {
        Path projectDirectory = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, PREVIEW_KEY);
        if (Files.exists(projectDirectory)) {
            try (var paths = Files.walk(projectDirectory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

    @Test
    void servesViteJavaScriptWithJavaScriptMimeType() throws IOException {
        Path javascriptFile = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, PREVIEW_KEY, "dist", "assets", "app.js");
        Files.createDirectories(javascriptFile.getParent());
        Files.writeString(javascriptFile, "export default {};");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping"))
                .thenReturn("/static/" + PREVIEW_KEY + "/dist/assets/app.js");

        ResponseEntity<?> response = new StaticResourceController().serveStaticResource(PREVIEW_KEY, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .isEqualTo("application/javascript; charset=UTF-8");
    }

    @Test
    void servesVueProductionEntryWhenPreviewRequestsDistDirectory() throws IOException {
        Path entryFile = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, PREVIEW_KEY, "dist", "index.html");
        Files.createDirectories(entryFile.getParent());
        Files.writeString(entryFile, "<div id=\"app\">Vue preview</div>");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping"))
                .thenReturn("/static/" + PREVIEW_KEY + "/dist/");

        ResponseEntity<?> response = new StaticResourceController().serveStaticResource(PREVIEW_KEY, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .isEqualTo("text/html; charset=UTF-8");
        assertThat(response.getBody()).isInstanceOf(FileSystemResource.class);
    }
}
