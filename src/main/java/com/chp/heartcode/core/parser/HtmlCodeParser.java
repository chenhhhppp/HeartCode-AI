package com.chp.heartcode.core.parser;

import com.chp.heartcode.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: CHP
 * @Description: 单文件代码解析器
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    // 宽容匹配 HTML：允许开头没有 ```，允许结尾是 ``` 或 ``n 或直接结束
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "(?:```\\s*(?:html|htm)\\s*\\n|(?:html|htm)\\s*\\n)([\\s\\S]*?)(?:\\n\\s*```|\\n\\s*``n|(?=\\n\\s*###)|$)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else {
            //如果没有找到代码块，将整个内容作为HTML
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 提取 HTML 代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private static String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
