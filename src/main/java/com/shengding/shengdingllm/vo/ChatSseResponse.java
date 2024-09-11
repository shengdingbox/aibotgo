package com.shengding.shengdingllm.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.shengding.shengdingllm.cosntant.ChatMessageRoleEnum.USER;

@Data
public class ChatSseResponse {

    private String id;
    private String model;
    private String object = "chat.completion.chunk";
    private List<ChoicesResponse> choices;
    private UsageResponse usage;
    private int created;

    public ChatSseResponse(String id, String model, List<ChoicesResponse> choices) {
        this.id = id;
        this.model = model;
        this.choices = choices;
        this.created = (int)(System.currentTimeMillis() / 1000);
    }

    public static String streamToString(String id, String model, String content) {
        ChoicesResponse choicesResponse = new ChoicesResponse(content);
        List<ChoicesResponse> list = new ArrayList<>();
        list.add(choicesResponse);
        ChatSseResponse chatSseResponse = new ChatSseResponse(id, model, list);
        return JSONObject.toJSONString(chatSseResponse);
    }

    public static String stopStreamToString(String id, String model, String userMessage, String content) {
        UsageResponse usageResponse = new UsageResponse();
        usageResponse.setPrompt_tokens(userMessage.split("\r\n").length);
        usageResponse.setCompletion_tokens(content.split("\r\n").length);
        usageResponse.setTotal_tokens(content.split("\r\n").length + userMessage.split("\r\n").length);
        ChoicesResponse choicesResponse = new ChoicesResponse(true);
        List<ChoicesResponse> list = new ArrayList<>();
        list.add(choicesResponse);
        ChatSseResponse chatSseResponse = new ChatSseResponse(id, model, list);
        chatSseResponse.setUsage(usageResponse);
        return JSONObject.toJSONString(chatSseResponse);
    }
    public static String chatString(String id, String model, String userMessage, String content) {
        UsageResponse usageResponse = new UsageResponse();
        usageResponse.setPrompt_tokens(userMessage.split("\r\n").length);
        usageResponse.setCompletion_tokens(content.split("\r\n").length);
        usageResponse.setTotal_tokens(content.split("\r\n").length + userMessage.split("\r\n").length);
        ChoicesResponse choicesResponse = new ChoicesResponse(USER.getValue(),content);
        List<ChoicesResponse> list = new ArrayList<>();
        list.add(choicesResponse);
        ChatSseResponse chatSseResponse = new ChatSseResponse(id, model, list);
        chatSseResponse.setUsage(usageResponse);
        return JSONObject.toJSONString(chatSseResponse);
    }


}
