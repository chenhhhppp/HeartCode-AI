package com.chp.heartcode.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 对话记忆存储配置类。
 * <p>
 * 初始化 RedisChatMemoryStore Bean，用于将 LangChain4j 的对话记忆持久化到 Redis。
 * 本地 Redis 未设置密码，因此不配置 password 参数。
 *
 * @author CHP
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    /**
     * Redis 主机地址
     */
    private String host;

    /**
     * Redis 端口
     */
    private int port;

    /**
     * 对话记忆存活时间（秒）
     */
    private long ttl;

    /**
     * 初始化 RedisChatMemoryStore Bean
     *
     * @return RedisChatMemoryStore 实例
     */
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .ttl(ttl)
                .build();
    }
}
