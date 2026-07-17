package com.chp.heartcode.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * @Author: CHP
 * @Description: html代码结果
 */
@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {

    @Description("HTML代码")
    private String htmlCode;

    @Description("HTML代码描述")
    private String description;
}
