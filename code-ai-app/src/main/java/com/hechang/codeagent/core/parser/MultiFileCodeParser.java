package com.hechang.codeagent.core.parser;

import com.hechang.codeagent.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多文件代码解析器（HTML + CSS + JS）
 *
 * @author chang
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    /**
     * 允许模型在语言标签后补充文件名，例如 ```css style.css，兼容 CRLF 与 LF 换行。
     */
    private static final Pattern HTML_CODE_PATTERN = createCodeBlockPattern("html|htm");
    private static final Pattern CSS_CODE_PATTERN = createCodeBlockPattern("css");
    private static final Pattern JS_CODE_PATTERN = createCodeBlockPattern("js|javascript|mjs");

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        // 提取各类代码
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        // 设置HTML代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置CSS代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置JS代码
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static Pattern createCodeBlockPattern(String language) {
        return Pattern.compile(
                "(?ims)^[\\t ]*```[\\t ]*(?:" + language + ")(?:[\\t ]+[^\\r\\n`]*)?[\\t ]*\\R(.*?)^[\\t ]*```[\\t ]*$");
    }
}
