package com.shengding.shengdingllm.vo;


import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@Data
public class SseAskParams {

    private String modelName;

    private String apiKey;

    private String regenerateQuestionUuid;

    private SseEmitter sseEmitter;

    private Boolean stream;

    private Boolean useSearch;

    /**
     * 组装LLMService所属的属性，非必填
     */
    private LLMBuilderProperties llmBuilderProperties;

    /**
     * 最终提交给llm的信息，必填
     */
    private AssistantChatParams assistantChatParams;
}
