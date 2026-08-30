package com.hechang.codeagent.core.saver;

import cn.hutool.core.util.StrUtil;
import com.hechang.codeagent.ai.model.MultiFileCodeResult;
import com.hechang.codeagent.exception.BusinessException;
import com.hechang.codeagent.exception.ErrorCode;
import com.hechang.codeagent.model.enums.CodeGenTypeEnum;

/**
 * 多文件代码保存器
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 文件
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JavaScript 文件
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 多文件模式的契约是 HTML、CSS、JS 三个完整文件。缺失任一文件时不能生成一个
        // 看似成功、实际以浏览器默认样式渲染的页面。
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
        if (StrUtil.isBlank(result.getCssCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "多文件代码不完整：缺少 style.css 内容，请重新生成");
        }
        if (StrUtil.isBlank(result.getJsCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "多文件代码不完整：缺少 script.js 内容，请重新生成");
        }
    }
}
