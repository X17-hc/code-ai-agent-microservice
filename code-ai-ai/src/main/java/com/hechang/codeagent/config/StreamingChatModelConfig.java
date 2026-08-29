package com.hechang.codeagent.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 流式模型配置
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
@Slf4j
public class StreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Boolean logRequests;
    private Boolean logResponses;


    /**
     * 推理流式模型（用于 Vue 项目生成，带工具调用）
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype() {
        // 使用配置文件中的模型名称和参数
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
