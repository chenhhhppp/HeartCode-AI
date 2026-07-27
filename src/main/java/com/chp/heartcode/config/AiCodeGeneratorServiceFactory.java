package com.chp.heartcode.config;

import com.chp.heartcode.ai.AiCodeGeneratorService;
import com.chp.heartcode.ai.guardrail.PromptSafetyInputGuardrail;
import com.chp.heartcode.ai.tools.ToolManager;
import com.chp.heartcode.exception.BusinessException;
import com.chp.heartcode.exception.ErrorCode;
import com.chp.heartcode.model.enums.CodeGenTypeEnum;
import com.chp.heartcode.service.ChatHistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;

/**
 * AI 代码生成服务工厂。
 * <p>
 * 为每个应用（appId）分配专属的 AiCodeGeneratorService 实例，
 * 每个 Service 绑定独立的 MessageWindowChatMemory，彻底隔离不同应用的对话上下文。
 * <p>
 * 缓存策略（Caffeine）：
 * <ul>
 *   <li>最大缓存 1000 个实例（防止内存溢出）</li>
 *   <li>写入后 30 分钟过期（长期空闲的应用释放资源）</li>
 *   <li>访问后 10 分钟过期（热点应用保持更久）</li>
 *   <li>移除时打印日志，便于排查</li>
 * </ul>
 *
 * @author CHP
 */
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    /**
     * Spring 应用上下文，用于每次创建 AI 服务时从容器获取全新的多例（prototype）模型实例。
     * <p>
     * 模型 Bean 已声明为 @Scope("prototype")，每次 getBean 都返回独立实例，
     * 底层 HttpClient 不再共享，从而彻底解决并发串行阻塞问题。
     */
    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 对话历史服务，用于从数据库加载历史对话到记忆中。
     * <p>
     * 使用 @Lazy 打破循环依赖：
     * Factory → ChatHistoryService → AppService → AiCodeGeneratorFacade → Factory
     */
    @Resource
    @Lazy
    private ChatHistoryService chatHistoryService;

    /**
     * 工具管理器，统一管理所有 AI 工具
     */
    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）这个方法是为了兼容历史逻辑
     *
     * @param appId 应用 ID，作为对话记忆的隔离 key
     * @return 与该应用绑定的 AiCodeGeneratorService
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     *
     * @param appId       应用 ID，作为对话记忆的隔离 key
     * @param codeGenType 代码生成类型，决定使用哪种模型配置
     * @return 与该应用绑定的 AiCodeGeneratorService
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 创建新的 AI 服务实例。
     * <p>
     * 根据代码生成类型选择不同的模型配置：
     * <ul>
     *   <li>VUE_PROJECT：使用推理流式模型（reasoningStreamingChatModel），绑定 FileWriteTool 工具，
     *       通过 chatMemoryProvider 为每个 memoryId 绑定会话记忆（满足 @MemoryId 要求）</li>
     *   <li>HTML / MULTI_FILE：使用默认模型（chatModel + defaultStreamingChatModel）</li>
     * </ul>
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例，生成类型: {}", appId, codeGenType);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        // 每次从 Spring 容器获取全新的多例模型实例（prototype），彻底避免并发阻塞
        return switch (codeGenType) {
            // Vue 项目生成使用推理模型，通过工具管理器绑定全套文件操作工具
            case VUE_PROJECT -> {
                StreamingChatModel reasoningModel = applicationContext.getBean("reasoningStreamingChatModel", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools((Object[]) toolManager.getAllTools())
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();
            }
            // HTML 和多文件生成使用默认模型
            case HTML, MULTI_FILE -> {
                ChatModel chatModel = applicationContext.getBean("chatModel", ChatModel.class);
                StreamingChatModel defaultStreamingModel = applicationContext.getBean("defaultStreamingChatModel", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(defaultStreamingModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 默认提供一个 Bean（appId=0），保证与之前代码的注入兼容。
     * <p>
     * 注意：此 Bean 仅用于未传 appId 的兼容场景（如废弃方法、测试）。
     * 正式业务请通过工厂的 getAiCodeGeneratorService(appId, codeGenType) 获取专属实例。
     *
     * @return 默认的 AiCodeGeneratorService（绑定 appId=0 的记忆）
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }
}
