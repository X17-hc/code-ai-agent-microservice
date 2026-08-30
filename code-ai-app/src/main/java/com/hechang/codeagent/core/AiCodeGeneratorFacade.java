package com.hechang.codeagent.core;

import cn.hutool.json.JSONUtil;
import com.hechang.codeagent.ai.AiCodeGeneratorService;
import com.hechang.codeagent.ai.AiCodeGeneratorServiceFactory;
import com.hechang.codeagent.ai.model.HtmlCodeResult;
import com.hechang.codeagent.ai.model.MultiFileCodeResult;
import com.hechang.codeagent.ai.model.message.AiResponseMessage;
import com.hechang.codeagent.ai.model.message.ToolExecutedMessage;
import com.hechang.codeagent.ai.model.message.ToolRequestMessage;
import com.hechang.codeagent.constant.AppConstant;
import com.hechang.codeagent.core.builder.VueProjectBuilder;
import com.hechang.codeagent.core.parser.CodeParserExecutor;
import com.hechang.codeagent.core.saver.CodeFileSaverExecutor;
import com.hechang.codeagent.exception.BusinessException;
import com.hechang.codeagent.exception.ErrorCode;
import com.hechang.codeagent.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    private static final Duration VUE_STREAM_INACTIVITY_TIMEOUT = Duration.ofMinutes(3);

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Autowired
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        // 根据 appId 获取相应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        // 根据 appId 获取相应的 AI 服务实例
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
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        AtomicBoolean terminalHandled = new AtomicBoolean(false);
        return Flux.<String>create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        if (terminalHandled.get()) {
                            return;
                        }
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        if (terminalHandled.get()) {
                            return;
                        }
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        if (terminalHandled.get()) {
                            return;
                        }
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        if (!terminalHandled.compareAndSet(false, true)) {
                            return;
                        }
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                        boolean buildSuccess = vueProjectBuilder.buildProject(projectPath);
                        if (!buildSuccess) {
                            sink.error(new BusinessException(ErrorCode.SYSTEM_ERROR,
                                    "Vue 项目生成或构建失败，请检查是否已成功写入 package.json 和源文件"));
                            return;
                        }
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        if (!terminalHandled.compareAndSet(false, true)) {
                            return;
                        }
                        sink.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成失败：" + error.getMessage()));
                    })
                    .start();
        }).timeout(VUE_STREAM_INACTIVITY_TIMEOUT)
                .onErrorResume(TimeoutException.class, error -> {
                    if (!terminalHandled.compareAndSet(false, true)) {
                        return Mono.empty();
                    }
                    log.warn("Vue 项目生成流连续 {} 分钟无输出，尝试构建已写入的项目: appId={}",
                            VUE_STREAM_INACTIVITY_TIMEOUT.toMinutes(), appId);
                    return buildVueProjectAfterInactivity(appId);
                });
    }

    /**
     * 上游模型流在文件已写入后失去响应时，尝试构建现有项目，避免前端无限等待。
     */
    Mono<String> buildVueProjectAfterInactivity(Long appId) {
        return Mono.fromCallable(() -> {
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                    return vueProjectBuilder.buildProject(projectPath);
                })
                .flatMap(buildSuccess -> {
                    if (buildSuccess) {
                        return Mono.empty();
                    }
                    return Mono.error(new BusinessException(ErrorCode.SYSTEM_ERROR,
                            "模型长时间未响应，且已生成的 Vue 项目构建失败"));
                });
    }

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        // 字符串拼接器，用于在流式返回所有代码之后保存文件。
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .doOnNext(codeBuilder::append)
                // 保存失败必须传递给 SSE 客户端，不能只记录日志后让前端显示失真的预览。
                .concatWith(Mono.defer(() -> {
                    String completeCode = codeBuilder.toString();
                    Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                    File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                    log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
                    return Mono.empty();
                }));
    }
}
