package com.chp.heartcode.core;

import com.chp.heartcode.ai.AiCodeGeneratorService;
import com.chp.heartcode.ai.model.HtmlCodeResult;
import com.chp.heartcode.ai.model.MultiFileCodeResult;
import com.chp.heartcode.ai.model.message.AiResponseMessage;
import com.chp.heartcode.ai.model.message.StreamMessageTypeEnum;
import com.chp.heartcode.ai.model.message.ToolExecutedMessage;
import com.chp.heartcode.ai.model.message.ToolRequestMessage;
import com.chp.heartcode.config.AiCodeGeneratorServiceFactory;
import com.chp.heartcode.core.parser.CodeParserExecutor;
import com.chp.heartcode.core.saver.CodeFileSaverExecutor;
import com.chp.heartcode.exception.BusinessException;
import com.chp.heartcode.exception.ErrorCode;
import com.chp.heartcode.model.enums.CodeGenTypeEnum;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * @Author: CHP
 * @Description: AI 代码生成外观类，组合生成和保存功能。
 * <p>
 * 通过 AiCodeGeneratorServiceFactory 为每个 appId 获取专属的 AI Service 实例，
 * 保证不同应用的对话记忆相互隔离。
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID（作为对话记忆隔离 key）
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiCodeGeneratorService service = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = service.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = service.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    // Reactor

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID（作为对话记忆隔离 key）
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型不能为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                // Vue 工程模式下，文件由 FileWriteTool 工具直接写入磁盘，
                // TokenStream 能实时暴露 AI 文本 + 工具调用事件，通过 processTokenStream 转为 Flux
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    // 辅助方法：如果大模型返回了 JSON，提取其中的 answer 字段；如果不是 JSON，原样返回
    public String extractAnswerFromJson(String rawContent) {
        try {
            // 使用 Hutool 的 JSONUtil 解析
            cn.hutool.json.JSONObject jsonObject = cn.hutool.json.JSONUtil.parseObj(rawContent);
            if (jsonObject.containsKey("answer")) {
                return jsonObject.getStr("answer");
            }
        } catch (Exception e) {
            // 如果解析失败，说明不是 JSON 格式，直接返回原内容
            log.warn("大模型返回内容非 JSON 格式，按原始 Markdown 处理");
        }
        return rawContent;
    }

    /**
     * 处理流式生成的代码：实时收集 + 完成后解析保存
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    // 流式返回完成后保存代码
                    try {
                        String rawContent = codeBuilder.toString();
                        String completeCode = extractAnswerFromJson(rawContent);

                        // 使用执行器解析代码
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);

                        log.info("大模型返回的完整内容：\n {}", completeCode);

                        // 使用执行器保存代码
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                        log.info("保存成功，路径为: {}", savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败: {}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onCompleteToolExecutionRequest((index, toolExecutionRequest) -> {
                        // 工具请求完成时（AI 已生成完整参数），立即发送完整的工具参数
                        // 此时 arguments 包含完整的文件路径和代码内容，无需等待工具执行完成
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage();
                        toolExecutedMessage.setType(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
                        toolExecutedMessage.setId(toolExecutionRequest.id());
                        toolExecutedMessage.setName(toolExecutionRequest.name());
                        toolExecutedMessage.setArguments(toolExecutionRequest.arguments());
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                        log.info("工具请求完成，已发送代码内容，工具名: {}", toolExecutionRequest.name());
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        log.error("AI 流式生成出错", error);
                        sink.error(error);
                    })
                    .start();
        });
    }
}
