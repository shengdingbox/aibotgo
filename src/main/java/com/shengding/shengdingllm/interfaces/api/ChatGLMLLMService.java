package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import io.micrometer.common.lang.Nullable;
import okhttp3.*;
import okhttp3.sse.EventSource;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.shengding.shengdingllm.cosntant.AdiConstant.CHAT_IO;

public class ChatGLMLLMService extends AbstractLLMService {
    private static final String GLM3 = "GLM-3";
    private static final String GLM4 = "GLM-4";

    public ChatGLMLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.CHATGLM;
        BASE_URL = "https://chatglm.cn";
    }


    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(50, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .hostnameVerifier((hostname, session) -> true)
            .build();


    Headers getAuthHeader() {
        return new Headers.Builder()
                .add("Authorization", "Bearer " + access_token)
                .build();
    }

    @Override
    public boolean checkAvailability() {
        boolean available = false;
        String userInfoUrl = BASE_URL + "/chatglm/backend-api/v3/user/info";
        Request request = new Request.Builder()
                .url(userInfoUrl)
                .get()
                .headers(getAuthHeader())
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JSONObject data = JSON.parseObject(responseBody);
                available = "success".equals(data.get("message"));
            }
        } catch (Exception e) {
            System.err.println("Error checking ChatGLM login status: " + e.getMessage());
        }

        return available;
    }

    @Override
    public void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        if (GLM3.equals(MODEL_NAME)) {
            sendPrompt3(assistantChatParams, onUpdateResponse, callbackParam);
        } else {
            sendPrompt4(assistantChatParams, onUpdateResponse, callbackParam);
        }
    }

    private void sendPrompt4(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        checkAvailability();
        //context = createChatContext(assistantChatParams.getMessageId());
        final String[] chatId = {StringUtils.isEmpty(assistantChatParams.getMessageId()) ? "" : assistantChatParams.getMessageId()};

        String url = BASE_URL + "/chatglm/backend-api/assistant/stream";
        String jsonPayload = JSON.toJSONString(Map.of(
                "assistant_id", "65940acff94777010aa6b796",
                "conversation_id", chatId[0],
                "meta_data", Map.of(
                        "is_test", false,
                        "input_question_type", "xxxx",
                        "channel", "",
                        "draft_id", ""
                ),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", assistantChatParams.getUserMessage().getContent()
                        ))
                ))
        ));

        Request request = new Request.Builder()
                .url(url)
                .headers(getAuthHeader())
                .addHeader("Accept", "text/event-stream")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), jsonPayload))
                .build();
        final StringBuffer text = new StringBuffer();
        new EventSourceStreamListener(okHttpClient, request) {
            @Override
            public void onEvent(EventSource eventSource, @Nullable String id, @Nullable String type, String eventData) {
                if (eventData != null) {
                    String beginning = "";
                    JSONObject data = JSONObject.parseObject(eventData);
                    JSONArray parts = data.getJSONArray("parts");
                    if (parts.size() == 0) {
                        onUpdateResponse.accept(callbackParam, createResponse(chatId[0], "", false));
                    } else {
                        JSONObject responsePart = parts.getJSONObject(0);
                        chatId[0] = data.getString("conversation_id");
                        if (responsePart == null || !"assistant".equals(responsePart.get("role"))) {
                            return;
                        }

                        String body = "";
                        String ending = "";
                        JSONArray content1 = responsePart.getJSONArray("content");
                        if (content1.size() == 0) {
                            return;
                        }
                        JSONObject content = content1.getJSONObject(0);
                        if (content == null) {
                            return;
                        }

                        if ("tool_calls".equals(content.get("type")) && "init".equals(responsePart.get("status"))) {
                            if ("browser".equals(content.getJSONObject("tool_calls").getString("name"))) {
                                String info = content.getJSONObject("tool_calls").getString("arguments");
                                if (info.startsWith("search")) {
                                    beginning += "> " + info + "\n";
                                }
                            }
                        } else if ("text".equals(content.get("type"))) {
                            body = (String) content.get("text");
                            if (null == body) {
                                return;
                            }
                            body = body.replace(text.toString(), "");
                            text.append(body);
                            JSONArray citations = responsePart.getJSONObject("meta_data").getJSONArray("citations");
                            if (citations != null) {
                                for (Object citation1 : citations) {
                                    JSONObject citation = (JSONObject) citation1;
                                    ending += "> 1. [" + citation.getJSONObject("metadata").getString("title") + "](" + citation.getJSONObject("metadata").getString("url") + ")\n";
                                }
                            }
                        }
                        boolean done = "finish".equals(data.get("status"));
                        onUpdateResponse.accept(callbackParam, createResponse(chatId[0], done ? text.toString() : body, done));

                    }
                }
            }
        };
    }

    private void sendPrompt3(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        try {
            Map<String, Object> context = createChatContext(assistantChatParams.getMessageId());
            if (context.get("user_id") == null) {
                checkAvailability();
                context = createChatContext(assistantChatParams.getMessageId());
            }
            String uuid = UUID.randomUUID().toString();
            String streamContextUrl = BASE_URL + "/chatglm/backend-api/v1/stream_context?__requestid=" + uuid;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("prompt", assistantChatParams.getUserMessage().getContent());
            jsonObject.put("seed", (int) (Math.random() * 100000));
            jsonObject.put("max_tokens", 512);
            String chatId = String.valueOf(context.get("chatId"));
            jsonObject.put("conversation_task_id", chatId);
            jsonObject.put("retry", false);
            jsonObject.put("retry_history_task_id", null);
            jsonObject.put("institution", "");
            jsonObject.put("__userid", context.get("user_id"));
            Request request = new Request.Builder()
                    .url(streamContextUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(MediaType.parse("application/json"), jsonObject.toJSONString()))
                    .headers(getAuthHeader())
                    .build();
            new EventSourceStreamListener(okHttpClient, request) {
                @Override
                public void onEvent(EventSource eventSource, @org.jetbrains.annotations.Nullable String id, @org.jetbrains.annotations.Nullable String type, String eventData) {
                    if (eventData != null) {
                        String line;
                        while ((line = eventData) != null) {
                            if (line.contains("data:")) {
                                String data = line.substring(line.indexOf("data:") + 5).trim();
                                onUpdateResponse.accept(callbackParam, createResponse(chatId, data, false));
                            }
                            if (line.contains("event:finish")) {
                                onUpdateResponse.accept(callbackParam, createResponse(chatId, "eventData", true));
                                break;
                            }
                        }
                    }
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject createChatContext(String chatId) {
        JSONObject jsonObject = new JSONObject();
        if (StringUtils.isBlank(chatId)) {
            String url = BASE_URL + "/chatglm/backend-api/v1/conversation";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                    .headers(getAuthHeader())
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject data = JSON.parseObject(responseBody);
                    jsonObject.put(CHAT_IO, data.getJSONObject("result").get("task_id"));
                    return jsonObject;
                }
            } catch (Exception e) {
                System.err.println("Error creating ChatGLM context: " + e.getMessage());
            }
        }
        jsonObject.put(CHAT_IO, chatId);
        return jsonObject;
    }
}
