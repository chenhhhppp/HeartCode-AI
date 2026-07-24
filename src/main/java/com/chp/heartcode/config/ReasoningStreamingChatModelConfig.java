package com.chp.heartcode.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 流式模型配置。
 * <p>
 * 由于 Spring DevTools 的 restart classloader 与 base classloader 会加载不同的 Class 对象，
 * 直接通过 @Resource 注入 starter auto-config 注册的 openAiStreamingChatModel Bean 会报类型不匹配。
 * 因此在这里自行创建流式模型 Bean，确保 Bean 创建者和使用者在同一个 classloader 中。
 *
 * @author CHP
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    /**
     * 默认流式模型（用于 HTML / 多文件生成）
     * <p>
     * 读取 streaming-chat-model 的配置属性
     */
    @Bean
    public StreamingChatModel defaultStreamingChatModel(
            @Value("${langchain4j.open-ai.streaming-chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.streaming-chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.streaming-chat-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.streaming-chat-model.max-tokens}") Integer maxTokens
    ) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 推理流式模型（用于 Vue 项目生成，带工具调用）
     * <p>
     * 复用 chat-model 的 apiKey / baseUrl 配置
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        // 为了测试方便临时修改
        final String modelName = "glm-5.2";
        final int maxTokens = 90000;
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .timeout(Duration.ofMinutes(30))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
