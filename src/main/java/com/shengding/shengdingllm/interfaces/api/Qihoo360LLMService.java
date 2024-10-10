package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.interfaces.EventSourceStreamListener;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
@Slf4j
@Service
public class Qihoo360LLMService extends AbstractLLMService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public Qihoo360LLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.QIHOO360;
        BASE_URL = "https://chat.360.com";
    }


    private Map<String, String> getAuthHeader() {
        Map<String, String> pairs = new HashMap<>();
        pairs.put("accept", "application/json, text/plain, */*");
        pairs.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,zh-HK;q=0.5,zh-TW;q=0.4");
        pairs.put("cache-control", "no-cache");
        //Q T
        pairs.put("cookie", access_token);
        pairs.put("pragma", "no-cache");
        pairs.put("priority", "u=1, i");
        pairs.put("referer", BASE_URL+"/chat/498b00734d81b13a");
        pairs.put("sec-ch-ua", "Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128");
        pairs.put("sec-ch-ua-mobile", "?0");
        pairs.put("sec-ch-ua-platform", "macOS");
        pairs.put("sec-fetch-dest", "empty");
        pairs.put("sec-fetch-mode", "cors");
        pairs.put("upgrade-insecure-requests", "1");
        pairs.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
        // 获取访问令牌
        return pairs;
    }

    public boolean checkAvailability() {
        boolean available = false;

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + "/backend-api/api/user/info")
                .get();
        getAuthHeader().forEach(builder::addHeader);
        Request request = builder
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                available = responseBody.contains("\"message\":\"OK\"");
            }
        } catch (IOException e) {
            log.error("Error checking 360Bot Chat login status: " + e.getMessage());
        }

        return available;
    }

    @Override
    public void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        final String[] chatId = {assistantChatParams.getMessageId()};
        String url = BASE_URL + "/backend-api/api/common/chat";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("is_regenerate", false);
        jsonObject.put("is_so", false);
        jsonObject.put("prompt", assistantChatParams.getUserMessage().getContent());
        jsonObject.put("role", "00000001");
        jsonObject.put("source_type", "prophet_web");
        String payload = jsonObject.toJSONString();
        Request.Builder post = new Request.Builder()
                .url(url)
                .post(RequestBody.create(payload, MediaType.parse("application/json")));
        getAuthHeader().forEach(post::addHeader);
        Request request = post
                .build();

        new EventSourceStreamListener(client, request) {
            StringBuilder responseContent = new StringBuilder();

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                if ("200".equals(type)) {
                    responseContent.append(data);
                    onUpdateResponse.accept(callbackParam, createResponse(chatId[0], data, false));
                } else if ("100".equals(type)) {
                    // Handle CONVERSATIONID
                    chatId[0] = data.split("####")[1];
                    log.info("Conversation ID: " + chatId);
                } else if ("101".equals(type)) {
                    // Handle MESSAGEID
                    String messageId = data.split("####")[1];
                    log.info("Message ID: " + messageId);
                } else if ("40042".equals(type)) {
                    // Handle unable to answer the user's question
                    responseContent.append(data);
                    onUpdateResponse.accept(callbackParam, createResponse(chatId[0], responseContent.toString(), true));
                }
            }
            @Override
            public void onClosed(EventSource eventSource) {
                onUpdateResponse.accept(callbackParam, createResponse(chatId[0], responseContent.toString(), true));
            }
        };
    }

    @Override
    protected JSONObject createChatContext(String chatId) {
        return null;
    }
}
