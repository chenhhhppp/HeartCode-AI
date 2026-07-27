package com.chp.heartcode.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chp.heartcode.ai.model.message.AiResponseMessage;
import com.chp.heartcode.ai.model.message.StreamMessage;
import com.chp.heartcode.ai.model.message.StreamMessageTypeEnum;
import com.chp.heartcode.ai.model.message.ToolExecutedMessage;
import com.chp.heartcode.ai.model.message.ToolRequestMessage;
import com.chp.heartcode.ai.tools.BaseTool;
import com.chp.heartcode.ai.tools.ToolManager;
import com.chp.heartcode.model.entity.User;
import com.chp.heartcode.model.enums.MessageTypeEnum;
import com.chp.heartcode.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 *
 * @author CHP
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    /**
     * 工具管理器，根据工具名称获取工具实例
     */
    @Resource
    private ToolManager toolManager;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    try {
                        // 解析每个 JSON 消息块
                        return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                    } catch (Exception e) {
                        // 单个 chunk 解析失败不应中断整条流
                        log.error("解析消息块失败: {}", e.getMessage(), e);
                        return "";
                    }
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, MessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, MessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并返回工具信息
                    seenToolIds.add(toolId);
                    String toolName = toolRequestMessage.getName();
                    // 根据工具名称获取工具实例
                    BaseTool tool = toolManager.getTool(toolName);
                    if (tool != null) {
                        // 返回格式化的工具调用信息
                        return tool.generateToolRequestResponse();
                    } else {
                        // 未注册的工具，降级显示原始名称
                        return String.format("\n\n[选择工具] %s\n\n", toolName);
                    }
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                try {
                    ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                    String toolName = toolExecutedMessage.getName();
                    String arguments = toolExecutedMessage.getArguments();
                    if (StrUtil.isBlank(arguments)) {
                        log.warn("工具执行结果 arguments 为空，跳过");
                        return "";
                    }
                    JSONObject jsonObject = JSONUtil.parseObj(arguments);
                    // 根据工具名称获取工具实例并生成相应的结果格式
                    BaseTool tool = toolManager.getTool(toolName);
                    String result;
                    if (tool != null) {
                        result = tool.generateToolExecutedResult(jsonObject);
                    } else {
                        // 未注册的工具，降级处理
                        result = String.format("[工具调用] %s", toolName);
                    }
                    // 输出前端和要持久化的内容
                    String output = String.format("\n\n%s\n\n", result);
                    chatHistoryStringBuilder.append(output);
                    log.info("已展示工具调用结果，工具: {}", toolName);
                    return output;
                } catch (Exception e) {
                    log.error("解析工具执行结果失败: {}", e.getMessage(), e);
                    return "";
                }
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}
