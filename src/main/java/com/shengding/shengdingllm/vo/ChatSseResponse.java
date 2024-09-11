package com.shengding.shengdingllm.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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

    public static String stopStreamToString(String id, String model, UsageResponse usageResponse) {
        ChoicesResponse choicesResponse = new ChoicesResponse(true);
        List<ChoicesResponse> list = new ArrayList<>();
        list.add(choicesResponse);
        ChatSseResponse chatSseResponse = new ChatSseResponse(id, model, list);
        chatSseResponse.setUsage(usageResponse);
        return JSONObject.toJSONString(chatSseResponse);
    }


}
