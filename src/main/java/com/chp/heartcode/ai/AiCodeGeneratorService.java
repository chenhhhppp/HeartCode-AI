package com.chp.heartcode.ai;

import com.chp.heartcode.ai.model.HtmlCodeResult;
import com.chp.heartcode.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * @Author: CHP
 * @Description: AI 代码生成服务。
 * <p>
 * 每个 appId 会有一个独立的 Service 实例（由 AiCodeGeneratorServiceFactory 创建），
 * 每个实例绑定独立的 MessageWindowChatMemory，实现对话上下文的应用级隔离。
 */
public interface AiCodeGeneratorService {

    /**
     * 生成代码（非流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码内容
     */
    String generateCode(@UserMessage String userMessage);

    /**
     * 生成 html 代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(@UserMessage String userMessage);

    /**
     * 生成 多文件 代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(@UserMessage String userMessage);

    /**
     * 生成 html 代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(@UserMessage String userMessage);

    /**
     * 生成 多文件 代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(@UserMessage String userMessage);

    /**
     * 生成 Vue 项目代码（流式）
     * <p>
     * 通过 @MemoryId 携带 appId，使工具调用（如 FileWriteTool）能通过 @ToolMemoryId 获取到 appId，
     * 从而为每个应用构建独立的文件保存目录。注意：使用 @MemoryId 后，
     * AiServices 构建时必须指定 chatMemoryProvider（而非 chatMemory）。
     *
     * @param appId        应用 ID，作为对话记忆和工具上下文的隔离 key
     * @param userMessage  用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}
