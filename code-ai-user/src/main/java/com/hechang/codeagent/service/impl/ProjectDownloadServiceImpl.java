package com.hechang.codeagent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hechang.codeagent.exception.BusinessException;
import com.hechang.codeagent.exception.ErrorCode;
import com.hechang.codeagent.exception.ThrowUtils;
import com.hechang.codeagent.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 项目下载服务实现类
 */
@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    @Override
    public void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response) {
        // 1. 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR, "项目路径不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR, "下载文件名不能为空");

        File projectDir = new File(projectPath);
        ThrowUtils.throwIf(!projectDir.exists() || !projectDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "项目目录不存在");

        // 2. 设置响应头
        try {
            String zipFileName = downloadFileName + ".zip";
            response.setContentType("application/zip");
            response.setCharacterEncoding("UTF-8");
            // URL编码文件名，支持中文
            String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            // 3. 创建ZIP输出流
            try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
                compressDirectory(projectDir, projectDir.getParentFile(), zipOut);
                zipOut.finish();
                zipOut.flush();
            }

            log.info("项目下载成功，路径: {}, 文件名: {}", projectPath, zipFileName);
        } catch (IOException e) {
            log.error("项目下载失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目下载失败: " + e.getMessage());
        }
    }

    /**
     * 递归压缩目录
     *
     * @param fileToZip 要压缩的文件或目录
     * @param parentDir 父目录（用于计算相对路径）
     * @param zipOut    ZIP输出流
     * @throws IOException IO异常
     */
    private void compressDirectory(File fileToZip, File parentDir, ZipOutputStream zipOut) throws IOException {
        String fileName = fileToZip.getName();
        String relativePath = parentDir.toPath().relativize(fileToZip.toPath()).toString();

        if (fileToZip.isDirectory()) {
            // 处理目录：添加目录条目并递归压缩子文件
            if (!relativePath.isEmpty()) {
                zipOut.putNextEntry(new ZipEntry(relativePath + "/"));
                zipOut.closeEntry();
            }

            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File child : children) {
                    compressDirectory(child, parentDir, zipOut);
                }
            }
        } else {
            // 处理文件：添加文件条目并写入内容
            zipOut.putNextEntry(new ZipEntry(relativePath));
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, len);
                }
            }
            zipOut.closeEntry();
        }
    }
}
