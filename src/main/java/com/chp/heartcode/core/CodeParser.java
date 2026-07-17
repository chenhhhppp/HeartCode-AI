package com.chp.heartcode.core;

import com.chp.heartcode.ai.model.HtmlCodeResult;
import com.chp.heartcode.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: CHP
 * @Description: 代码解析器
 * 提供静态方法解析不同类型的代码内容
 */
public class CodeParser {

    // 优化后的正则表达式：
    // 1. ```(?:html|htm)\s*\n ：匹配开始标记，容忍 htm，容忍尾部空格和换行
    // 2. ([\s\S]*?)           ：非贪婪匹配代码内容
    // 3. \n\s*```             ：匹配结束标记。关键点：要求结束的 ``` 前面可以有换行和空格，但不能是字母数字，防止误判
/*
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```(?:html|htm)\\s*\\n([\\s\\S]*?)\\n\\s*```", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```(?:css|style)\\s*\\n([\\s\\S]*?)\\n\\s*```", Pattern.CASE_INSENSITIVE);

    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript|es6)\\s*\\n([\\s\\S]*?)\\n\\s*```", Pattern.CASE_INSENSITIVE);
*/

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


    /**
     * 解析 HTML 单文件代码
     *
     * @param codeContent
     * @return
     */
    public static HtmlCodeResult parseHtmlCode(String codeContent) {
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
     * 解析多文件代码（HTML+CSS+JS）
     *
     * @param codeContent
     * @return
     */
    public static MultiFileCodeResult parseMultiFileCode(String codeContent) {
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
