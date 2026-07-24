package com.chp.heartcode.core.parser;

import com.chp.heartcode.ai.model.MultiFileCodeResult;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: CHP
 * @Description: 多文件代码解析器 - 简单直接的解析方案
 */
@Slf4j
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        log.info("开始解析代码，内容长度: {}", codeContent != null ? codeContent.length() : 0);

        if (codeContent == null || codeContent.trim().isEmpty()) {
            log.error("代码内容为空");
            return new MultiFileCodeResult();
        }

        MultiFileCodeResult result = new MultiFileCodeResult();

        // 第一步：提取 HTML 代码
        String htmlCode = extractHtml(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
            log.info("提取 HTML 成功，长度: {}", htmlCode.length());
        }

        // 第二步：提取 CSS 代码
        String cssCode = extractCss(codeContent);
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
            log.info("提取 CSS 成功，长度: {}", cssCode.length());
        }

        // 第三步：提取 JS 代码
        String jsCode = extractJs(codeContent);
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
            log.info("提取 JS 成功，长度: {}", jsCode.length());
        }

        log.info("最终结果 - HTML: {}, CSS: {}, JS: {}",
                result.getHtmlCode() != null ? "OK" : "null",
                result.getCssCode() != null ? "OK" : "null",
                result.getJsCode() != null ? "OK" : "null");

        return result;
    }

    /**
     * 提取 HTML 代码
     * 从 <!DOCTYPE 或 <html> 开始到 </html> 结束
     */
    private String extractHtml(String content) {
        // 查找 HTML 开始位置
        int htmlStart = content.indexOf("<!DOCTYPE");
        if (htmlStart == -1) {
            htmlStart = content.toLowerCase().indexOf("<html");
        }

        if (htmlStart == -1) {
            log.warn("未找到 HTML 开始标记");
            return null;
        }

        // 从 HTML 开始位置截取
        String htmlContent = content.substring(htmlStart);

        // 查找 HTML 结束位置
        int htmlEnd = htmlContent.lastIndexOf("</html>");
        if (htmlEnd == -1) {
            htmlEnd = htmlContent.lastIndexOf("</HTML>");
        }

        if (htmlEnd == -1) {
            log.warn("未找到 HTML 结束标记，返回从开始到末尾");
            // 如果没有找到结束标记，返回到下一个文件标记之前的内容
            int nextFileMark = findNextFileMark(htmlContent);
            if (nextFileMark > 0) {
                return cleanCodeEnd(htmlContent.substring(0, nextFileMark));
            }
            return cleanCodeEnd(htmlContent);
        }

        // 返回完整的 HTML 内容（包含结束标签）
        return cleanCodeEnd(htmlContent.substring(0, htmlEnd + 7));
    }

    /**
     * 提取 CSS 代码
     * 匹配从 style.css 或 css 开始到下一个文件标记或结束
     */
    private String extractCss(String content) {
        // 查找 CSS 开始标记
        int cssStart = -1;

        // 尝试匹配 "style.css" 或 "css" 后跟换行
        Pattern[] patterns = {
                Pattern.compile("style\\.css\\r?\\n", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\.css\\r?\\n", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:^|\\r?\\n)css\\r?\\n", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
                Pattern.compile("```css\\r?\\n", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                cssStart = matcher.end();
                break;
            }
        }

        if (cssStart == -1) {
            log.debug("未找到 CSS 开始标记");
            return null;
        }

        // 从 CSS 开始位置截取
        String cssContent = content.substring(cssStart);

        // 查找 CSS 结束位置（下一个文件开始或 ``` 或 ###）
        int cssEnd = findCssEnd(cssContent);
        if (cssEnd > 0) {
            String css = cssContent.substring(0, cssEnd);
            // 清理开头和结尾
            return cleanCodeEnd(cleanCodeStart(css));
        }

        return cleanCodeEnd(cleanCodeStart(cssContent));
    }

    /**
     * 提取 JS 代码
     * 匹配从 script.js 或 javascript 开始到结束
     */
    private String extractJs(String content) {
        // 查找 JS 开始标记
        int jsStart = -1;

        // 尝试匹配 "script.js" 或 "javascript" 后跟换行
        Pattern[] patterns = {
                Pattern.compile("script\\.js\\r?\\n", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\.js\\r?\\n", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:^|\\r?\\n)javascript\\r?\\n", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
                Pattern.compile("(?:^|\\r?\\n)js\\r?\\n", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
                Pattern.compile("```(?:js|javascript)\\r?\\n", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                jsStart = matcher.end();
                break;
            }
        }

        if (jsStart == -1) {
            log.debug("未找到 JS 开始标记");
            return null;
        }

        // 从 JS 开始位置截取
        String jsContent = content.substring(jsStart);

        // 查找 JS 结束位置（``` 或 ###）
        int jsEnd = findJsEnd(jsContent);
        if (jsEnd > 0) {
            return cleanCodeEnd(cleanCodeStart(jsContent.substring(0, jsEnd)));
        }

        return cleanCodeEnd(cleanCodeStart(jsContent));
    }

    /**
     * 查找下一个文件标记的位置
     */
    private int findNextFileMark(String content) {
        // 匹配文件名标记或 ### 或 ```
        Pattern pattern = Pattern.compile("\\r?\\n(?:style\\.css|script\\.js|###|```)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.start();
        }
        return -1;
    }

    /**
     * 查找 CSS 代码结束位置
     */
    private int findCssEnd(String content) {
        // CSS 结束标记：下一个文件、``` 或 ###
        int end1 = content.indexOf("\nscript.js");
        int end2 = content.indexOf("\n###");
        int end3 = content.indexOf("\n```");

        // 找到最早出现的结束标记
        int end = -1;
        if (end1 > 0 && (end == -1 || end1 < end)) end = end1;
        if (end2 > 0 && (end == -1 || end2 < end)) end = end2;
        if (end3 > 0 && (end == -1 || end3 < end)) end = end3;

        return end;
    }

    /**
     * 查找 JS 代码结束位置
     */
    private int findJsEnd(String content) {
        // JS 结束标记：``` 或 ###
        int end1 = content.indexOf("\n```");
        int end2 = content.indexOf("\n###");

        if (end1 > 0 && end2 > 0) {
            return Math.min(end1, end2);
        } else if (end1 > 0) {
            return end1;
        } else if (end2 > 0) {
            return end2;
        }

        return -1;
    }

    /**
     * 清理代码末尾的多余内容
     * 移除 ```x、``` 等结束标记
     */
    private String cleanCodeEnd(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }

        // 移除末尾的 ```x 或 ``` （包括 ```n、```css 等）
        code = code.replaceAll("\\r?\\n```[\\w]*\\r?\\n?$", "");
        code = code.replaceAll("\\r?\\n```[\\w]*$", "");

        // 移除末尾的 ### xxx
        code = code.replaceAll("\\r?\\n###.*$", "");

        // 移除末尾的语言标识符
        code = code.replaceAll("\\r?\\n(?:html|css|js|javascript|es6)$", "");

        return code.trim();
    }

    /**
     * 清理代码开头的多余内容
     * 移除语言标识符前缀
     */
    private String cleanCodeStart(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }

        // 移除开头的语言标识符（html、css、js、javascript、es6）
        code = code.replaceAll("^(?:html|css|js|javascript|es6)\\r?\\n", "");

        // 移除开头的文件名标记
        code = code.replaceAll("^文件名[:：][^\\r\\n]*\\r?\\n", "");

        return code.trim();
    }
}
