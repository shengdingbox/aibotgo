package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import okhttp3.*;
import okhttp3.sse.EventSource;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.shengding.shengdingllm.cosntant.AdiConstant.CHAT_IO;

public class MistralLLMService extends AbstractLLMService {


    public MistralLLMService(){
        MODEL_NAME = AdiConstant.ModelPlatform.MISTRAL;
        BASE_URL = "https://chat.mistral.ai";
    }



    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(50, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .hostnameVerifier((hostname, session) -> true)
            .build();

    @Override
    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        Map<String, Object> chatContext = createChatContext(assistantChatParams.getMessageId());
        JSONObject json = new JSONObject();
        String chatId = (String) chatContext.get(CHAT_IO);
        json.put("chatId", chatId);
        // Replace with the correct model value
        json.put("model", "mistral-model");
        json.put("messageInput", assistantChatParams.getUserMessage());
        json.put("messageId", UUID.randomUUID().toString());
        json.put("mode", "append");
        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(BASE_URL+"/api/chat")
                .post(body)
                .build();

        new EventSourceStreamListener(okHttpClient, request) {

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String eventData) {
                if (eventData != null) {
                    String[] responses = eventData.split("\n");
                    for (String line : responses) {
                        if (!line.isEmpty()) {
                            String[] parts = line.split(":", 2);
                            String content =  parts.length > 1 ? parts[1] : line;
                            onUpdateResponse.accept(callbackParam, createResponse(chatId, content, false));
                        }
                    }
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, "", true));
                }

            }
        };
    }

    @Override
    protected JSONObject createChatContext(String chatId) {
        JSONObject json = new JSONObject();
        if (StringUtils.isBlank(chatId)) {
            json.put("chatId", "");
            json.put("content", "");
            json.put("rag", false);
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/trpc/message.newChat?batch=1")
                    .post(body)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject result = JSONObject.parseObject(response.body().string());
                    JSONObject data = result.getJSONObject("0");
                    if (data != null && data.containsKey("result")) {
                        chatId = data.getJSONObject("result").getJSONObject("data").getString("chatId");
                        if (chatId == null) {
                            throw new RuntimeException("chatId is empty in newChat");
                        }
                    }
                } else {
                    System.err.println("Error MistralBot createNewChat: Unexpected response " + response);
                }
            } catch (Exception e) {
                System.err.println("Error creating ChatGLM context: " + e.getMessage());
            }
        }
        json.put(CHAT_IO, chatId);
        return json;
    }

    public boolean checkAvailability() {
        boolean available = false;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BASE_URL + "/chat")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                available = true;
            }
        } catch (IOException e) {
            System.err.println("Error MistralBot checkAvailability: " + e.getMessage());
        }

        return available;
    }
}
