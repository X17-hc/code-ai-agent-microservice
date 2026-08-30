package com.hechang.codeagent.core.parser;

import com.hechang.codeagent.ai.model.MultiFileCodeResult;
import com.hechang.codeagent.core.saver.MultiFileCodeFileSaverTemplate;
import com.hechang.codeagent.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultiFileCodeParserTest {

    @Test
    void shouldParseThreeBlocksWhenLanguageTagsContainFileNames() {
        String response = """
                ```html index.html
                <link rel=\"stylesheet\" href=\"style.css\">
                <script src=\"script.js\"></script>
                ```
                ```css style.css
                body { color: #123456; }
                ```
                ```javascript script.js
                console.log('ready');
                ```
                """;

        MultiFileCodeResult result = new MultiFileCodeParser().parseCode(response);

        assertEquals("body { color: #123456; }", result.getCssCode());
        assertEquals("console.log('ready');", result.getJsCode());
    }

    @Test
    void shouldRejectIncompleteMultiFileResultBeforeWritingFiles() {
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode("<link rel=\"stylesheet\" href=\"style.css\">");
        result.setCssCode("body {}");

        assertThrows(BusinessException.class,
                () -> new MultiFileCodeFileSaverTemplate().saveCode(result, 1L));
    }
}
