package com.chp.heartcode.core.parser;

import com.chp.heartcode.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: CHP
 * @Description: 多文件代码解析器
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    // 宽容匹配 HTML：允许开头没有 ```，允许结尾是 ``` 或 ``n 或直接结束
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "(?:```\\s*(?:html|htm)\\s*\\n|(?:html|htm)\\s*\\n)([\\s\\S]*?)(?:\\n\\s*```|\\n\\s*``n|(?=\\n\\s*###)|$)",
            Pattern.CASE_INSENSITIVE
    );

    // 宽容匹配 CSS：允许开头没有 ```，允许结尾是 ``` 或 ``n 或直接结束
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile(
            "(?:```\\s*(?:css|style)\\s*\\n|(?:css|style)\\s*\\n)([\\s\\S]*?)(?:\\n\\s*```|\\n\\s*``n|(?=\\n\\s*###)|$)",
            Pattern.CASE_INSENSITIVE
    );

    // 宽容匹配 JS：允许开头没有 ```，允许结尾是 ``` 或 ``n 或直接结束
    private static final Pattern JS_CODE_PATTERN = Pattern.compile(
            "(?:```\\s*(?:js|javascript|es6)\\s*\\n|(?:js|javascript|es6)\\s*\\n)([\\s\\S]*?)(?:\\n\\s*```|\\n\\s*``n|(?=\\n\\s*###)|$)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        // 提取各类代码
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        // 设置 HTML 代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置 CSS 代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置 JS 代码
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
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
