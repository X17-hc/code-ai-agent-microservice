package com.hechang.codeagent.core.parser;

import com.hechang.codeagent.ai.model.HtmlCodeResult;

/**
 * HTML单文件代码解析器
 */
public class HtmlCoderParser implements CodeParser<HtmlCodeResult> {

    /**
     * 解析HTML单文件代码
     * @param codeContent 代码内容
     * @return 解析后的代码结果
     */
    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        // 从完整字符串中提取 HTML 代码
        return com.hechang.codeagent.core.CodeParser.parseHtmlCode(codeContent);
    }

}
