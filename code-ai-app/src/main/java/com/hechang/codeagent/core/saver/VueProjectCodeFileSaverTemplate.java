package com.hechang.codeagent.core.saver;

import cn.hutool.core.io.FileUtil;
import com.hechang.codeagent.exception.BusinessException;
import com.hechang.codeagent.exception.ErrorCode;
import com.hechang.codeagent.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Vue 项目代码文件保存器
 * Vue 项目通过 AI 工具调用直接写入文件，此保存器负责返回项目目录路径
 */
@Slf4j
public class VueProjectCodeFileSaverTemplate extends CodeFileSaverTemplate<Void> {

    /**
     * 获取 Vue 项目目录路径
     *
     * @param appId 应用 ID
     * @return 项目目录
     */
    public File getProjectDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "appId不能为空");
        }

        // 构建项目目录路径：vue_project_{appId}
        String projectDirName = "vue_project_" + appId;
        Path projectRoot = Paths.get(FILE_SAVE_ROOT_DIR, projectDirName);
        String projectDirPath = projectRoot.toString();

        log.info("Vue 项目目录: {}", projectDirPath);

        // 检查目录是否存在（理论上应该已经由工具调用创建）
        if (!FileUtil.exist(projectDirPath)) {
            log.warn("Vue 项目目录不存在: {}", projectDirPath);
            // 创建目录以避免后续错误
            FileUtil.mkdir(projectDirPath);
        }

        return new File(projectDirPath);
    }

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.VUE_PROJECT;
    }

    @Override
    protected void saveFiles(Void result, String baseDirPath) {
        // Vue 项目通过 AI 工具直接写入文件，此处无需额外操作
        log.debug("Vue 项目文件已由 AI 工具写入，目录: {}", baseDirPath);
    }
}
