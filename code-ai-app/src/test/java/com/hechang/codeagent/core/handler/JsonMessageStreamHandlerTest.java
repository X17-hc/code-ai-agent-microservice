package com.hechang.codeagent.core.handler;

import com.hechang.codeagent.ai.model.message.AiResponseMessage;
import com.hechang.codeagent.ai.model.message.StreamMessageTypeEnum;
import com.hechang.codeagent.model.entity.User;
import com.hechang.codeagent.model.enums.ChatHistoryMessageTypeEnum;
import com.hechang.codeagent.service.ChatHistoryService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JsonMessageStreamHandlerTest {

    @Test
    void storesCompletionMessageAfterVueProjectStreamCompletes() {
        JsonMessageStreamHandler handler = new JsonMessageStreamHandler();
        ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
        User user = new User();
        user.setId(7L);
        AiResponseMessage message = new AiResponseMessage();
        message.setType(StreamMessageTypeEnum.AI_RESPONSE.getValue());
        message.setData("项目文件已生成");

        handler.handle(Flux.just(cn.hutool.json.JSONUtil.toJsonStr(message)), chatHistoryService, 3L, user).blockLast();

        verify(chatHistoryService).addChatMessage(
                eq(3L),
                eq("Vue 项目已生成并构建完成，可在右侧预览。"),
                eq(ChatHistoryMessageTypeEnum.AI.getValue()),
                eq(7L)
        );
    }
}
