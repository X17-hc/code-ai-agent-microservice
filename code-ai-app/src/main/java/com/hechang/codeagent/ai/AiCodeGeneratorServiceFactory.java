package com.hechang.codeagent.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hechang.codeagent.ai.guardrail.PromptSafetyInputGuardrail;
import com.hechang.codeagent.ai.guardrail.RetryOutputGuardrail;
import com.hechang.codeagent.ai.tools.ToolManager;
import com.hechang.codeagent.model.enums.CodeGenTypeEnum;
import com.hechang.codeagent.service.ChatHistoryService;
import com.hechang.codeagent.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 服务创建工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

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
            .removalListener((key, value, cause) -> log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause))
            .build();

    /**
     * 根据 appId 获取服务（为了兼容老逻辑）
     *
     * @param appId 应用 id
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取服务
     *
     * @param appId       应用 id
     * @param codeGenType 生成类型
     * @return AI 服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 创建新的 AI 服务实例
     *
     * @param appId       应用 id
     * @param codeGenType 生成类型
     * @return AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return switch (codeGenType) {
            // Vue 项目生成，使用工具调用和推理模型
            case VUE_PROJECT -> {
                // 使用多例模式的StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel =
                        SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .maxSequentialToolsInvocations(30) // 与提示词的“文件总数小于 30”约束保持一致
                        .chatMemoryProvider(memoryId -> chatMemory)
                        // 不要把 BaseTool[] 强转为单个 Object；否则可变参数只会收到“一个数组对象”，
                        // LangChain4j 无法从数组类中发现 @Tool 方法，最终请求不会携带 tools 字段。
                        .tools((Object[]) toolManager.getAllTools())
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                        .outputGuardrails(new RetryOutputGuardrail()) // 添加输出护轨-如果用了输出护轨，可能会导致流式输出的响应不及时，等到AI输出结束才一起返回，所以为了追求流式输出效果，建议不要通过护轨方式进行重试
                        // 处理工具调用幻觉问题
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: there is no tool called " + toolExecutionRequest.name())
                        )
                        .build();
            }
            // HTML 和 多文件生成，使用流式对话模型
            case HTML, MULTI_FILE -> {
                // 使用多例模式的StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel =
                        SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .maxSequentialToolsInvocations(20) //最多连续调用20次工具
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .build();
            }
        };
    }

    /**
     * 创建 AI 代码生成器服务
     *
     * @return AI 代码生成器服务实例
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }

    /**
     * 构造缓存键
     *
     * @param appId 应用 id
     * @param codeGenType 生成类型
     * @return 缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}
