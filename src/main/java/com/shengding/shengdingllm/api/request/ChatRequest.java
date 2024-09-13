package com.shengding.shengdingllm.api.request;

import com.shengding.shengdingllm.cosntant.ChatMessageRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatRequest {

    private String model;
    private List<Message> messages = new ArrayList<>();
    private boolean stream;
    private boolean use_search;

    public Message getSystemMessage() {
        return messages.stream().filter(message -> ChatMessageRoleEnum.SYSTEM.getValue().equals(message.getRole())).findFirst().orElse(new Message());
    }
    public Message getUserMessage() {
        return messages.stream().filter(message -> ChatMessageRoleEnum.USER.getValue().equals(message.getRole())).reduce((first, second) -> second).orElse(new Message());
    }


}
