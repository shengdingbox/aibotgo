package com.shengding.shengdingllm.vo;

import com.shengding.shengdingllm.api.request.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssistantChatParams {
    private String messageId;
    private Message systemMessage;
    private Message userMessage;
    private List<Message> messages;
}
