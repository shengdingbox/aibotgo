package com.shengding.shengdingllm.interfaces.api.proxy;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.interfaces.EventSourceStreamListener;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import okhttp3.*;
import okhttp3.sse.EventSource;

import java.io.IOException;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.shengding.shengdingllm.cosntant.AdiConstant.CHAT_IO;

public class ClaudeLLMService extends AbstractLLMService {

    private String orgId;

    public ClaudeLLMService(String orgId) {
        this.orgId = orgId;
        MODEL_NAME = AdiConstant.ModelPlatform.CLAUDE;
        BASE_URL = "https://claude.ai/";
    }

    @Override
    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        JSONObject context = createChatContext(assistantChatParams.getMessageId());
        if (context == null) {
            onUpdateResponse.accept(callbackParam, createResponse("", "Failed to create chat context", true));
            return;
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        String chatId = context.getString(CHAT_IO);
        String url = "https://claude.ai/api/organizations/" + orgId + "/chat_conversations/" + chatId + "/completion";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("attachments", new JSONArray());
        jsonObject.put("files", new JSONArray());
        jsonObject.put("prompt", assistantChatParams.getUserMessage().getContent());
        jsonObject.put("timezone", TimeZone.getDefault().getID());
        RequestBody requestBody = RequestBody.create(
                jsonObject.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Content-Type", "application/json")
                .build();

        new EventSourceStreamListener(client, request) {
            StringBuilder text = new StringBuilder();

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                JSONObject jsonData = JSONObject.parseObject(data);
                if (jsonData.containsKey("completion")) {
                    text.append(jsonData.getString("completion"));
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, text.toString(), false));
                }
            }
        };
    }

    @Override
    protected JSONObject createChatContext(String chatId) {
        OkHttpClient client = new OkHttpClient();
        String uuid = UUID.randomUUID().toString();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "");
        jsonObject.put("uuid", uuid);
        RequestBody requestBody = RequestBody.create(
                jsonObject.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url("https://claude.ai/api/organizations/" + orgId + "/chat_conversations")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                jsonObject.put(CHAT_IO, uuid);
                return jsonObject;
            } else {
                System.err.println("Error creating chat context: " + response);
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkAvailability() {
        if (orgId == null || orgId.isEmpty()) {
            return false;
        }

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://claude.ai/api/account")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
