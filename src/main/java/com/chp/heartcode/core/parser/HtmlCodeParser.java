package com.chp.heartcode.core.parser;

import com.chp.heartcode.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: CHP
 * @Description: 单文件代码解析器
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    // 匹配 HTML：支持多种格式
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "```(?:html|htm)\\r?\\n([\\s\\S]*?)\\r?\\n```|" +           // 标准markdown代码块
            "```(?:html|htm)\\r?\\n([\\s\\S]*?)\\r?\\n``\\w*"            // 带```n等变体结束标记
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
     * 支持多个捕获组，返回第一个匹配的非空组
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private static String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            // 检查所有捕获组，返回第一个非空的
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && !group.trim().isEmpty()) {
                    return group.trim();
                }
            }
        }
        return null;
    }
}
