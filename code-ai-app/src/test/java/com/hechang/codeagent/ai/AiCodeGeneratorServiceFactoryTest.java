package com.hechang.codeagent.ai;

import cn.hutool.json.JSONObject;
import com.hechang.codeagent.ai.tools.BaseTool;
import com.hechang.codeagent.ai.tools.ToolManager;
import com.hechang.codeagent.service.ChatHistoryService;
import com.hechang.codeagent.utils.SpringContextUtil;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiCodeGeneratorServiceFactoryTest {

    private Object originalApplicationContext;

    @BeforeEach
    void preserveSpringContext() {
        originalApplicationContext = ReflectionTestUtils.getField(SpringContextUtil.class, "applicationContext");
    }

    @AfterEach
    void restoreSpringContext() {
        ReflectionTestUtils.setField(SpringContextUtil.class, "applicationContext", originalApplicationContext);
    }

    @Test
    void shouldExposeEveryVueToolToTheStreamingModel() {
        CapturingStreamingChatModel streamingModel = new CapturingStreamingChatModel();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(eq("reasoningStreamingChatModelPrototype"), eq(StreamingChatModel.class)))
                .thenReturn(streamingModel);
        ReflectionTestUtils.setField(SpringContextUtil.class, "applicationContext", applicationContext);

        ToolManager toolManager = mock(ToolManager.class);
        when(toolManager.getAllTools()).thenReturn(new BaseTool[]{new WriteFileTool()});

        AiCodeGeneratorServiceFactory factory = new AiCodeGeneratorServiceFactory();
        RedisChatMemoryStore memoryStore = mock(RedisChatMemoryStore.class);
        Map<Object, List<ChatMessage>> storedMessages = new HashMap<>();
        when(memoryStore.getMessages(any())).thenAnswer(invocation ->
                storedMessages.getOrDefault(invocation.getArgument(0), List.of()));
        doAnswer(invocation -> {
            storedMessages.put(invocation.getArgument(0), new ArrayList<>(invocation.getArgument(1)));
            return null;
        }).when(memoryStore).updateMessages(any(), any());
        ReflectionTestUtils.setField(factory, "redisChatMemoryStore", memoryStore);
        ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
        doAnswer(invocation -> {
            MessageWindowChatMemory memory = invocation.getArgument(1);
            memory.add(UserMessage.from("生成一个企业网站"));
            return 1;
        }).when(chatHistoryService).loadChatHistoryToMemory(eq(1L), any(MessageWindowChatMemory.class), eq(20));
        ReflectionTestUtils.setField(factory, "chatHistoryService", chatHistoryService);
        ReflectionTestUtils.setField(factory, "toolManager", toolManager);

        AiCodeGeneratorService service = factory.getAiCodeGeneratorService(1L,
                com.hechang.codeagent.model.enums.CodeGenTypeEnum.VUE_PROJECT);
        TokenStream tokenStream = service.generateVueProjectCodeStream(1L, "生成一个企业网站");
        tokenStream.onPartialResponse(ignored -> {
        }).onError(error -> {
            throw new AssertionError(error);
        }).start();

        assertThat(streamingModel.request.toolSpecifications())
                .extracting(toolSpecification -> toolSpecification.name())
                .contains("writeFile");
    }

    private static final class CapturingStreamingChatModel implements StreamingChatModel {

        private ChatRequest request;

        @Override
        public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
            request = chatRequest;
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("This is a valid response from the test model."))
                    .metadata(ChatResponseMetadata.builder().tokenUsage(new TokenUsage()).build())
                    .build());
        }
    }

    private static final class WriteFileTool extends BaseTool {

        @Tool("写入文件")
        public String writeFile(String relativeFilePath, String content) {
            return "ok";
        }

        @Override
        public String getToolName() {
            return "writeFile";
        }

        @Override
        public String getDisplayName() {
            return "写入文件";
        }

        @Override
        public String generateToolExecutedResult(JSONObject arguments) {
            return "ok";
        }
    }
}
