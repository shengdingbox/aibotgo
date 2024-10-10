package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.shengding.shengdingllm.cosntant.AdiConstant.CHAT_IO;

@Slf4j
@Service
public class MosslLLMService extends AbstractLLMService {

    public MosslLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.MOSS;
        BASE_URL = "https://moss.fastnlp.top/moss";
    }
    private static final String LOGIN_URL = "/";

    private Request.Builder getAuthHeader() {
        return new Request.Builder()
                .addHeader("Authorization", "Bearer " + access_token);
    }

    @Override
    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {

        Map<String, Object> context = createChatContext(assistantChatParams.getMessageId());
        String chatId = (String) context.get(CHAT_IO);
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        String url = "wss://moss.fastnlp.top/api/ws/chats/" + chatId + "/records?jwt=" + access_token;
        Request request = new Request.Builder().url(url).build();
        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            String beginning = "";
            String body = "";
            String ending = "";

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                JSONObject data = new JSONObject();
                data.put("request", assistantChatParams.getUserMessage());
                webSocket.send(data.toString());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                JSONObject event = JSONObject.parseObject(text);
                if (!event.containsKey("status")) {
                    JSONArray links = event.getJSONArray("processed_extra_data");
                    if (links != null && !links.isEmpty()) {
                        JSONObject linkData = links.getJSONObject(0).getJSONObject("data");
                        for (String key : linkData.keySet()) {
                            JSONObject link = linkData.getJSONObject(key);
                            ending += "> " + key + ". [" + link.getString("title") + "](" + link.getString("url") + ")\n";
                        }
                    }

                    onUpdateResponse.accept(callbackParam, createResponse(chatId, beginning + "\n" + body + "\n" + ending, true));
                    webSocket.close(1000, null);
                } else {
                    int status = event.getInteger("status");
                    if (status == 1) {
                        body = event.getString("output");
                    } else if (status == 3 && "start".equals(event.getString("stage"))) {
                        beginning += "> " + event.getString("type") + " " + event.getString("output") + "\n";
                    } else if (status == -1) {
                        webSocket.close(1000, null);
                        Exception e = new Exception(event.getString("status_code") + " " + event.getString("output"));
                        onUpdateResponse.accept(chatId, createErrorResponse(e));
                    }

                    onUpdateResponse.accept(callbackParam, createResponse(chatId, beginning + "\n" + body + "\n" + ending, false));
                }
            }
        });
        client.dispatcher().executorService().shutdown();
    }

    @Override
    public JSONObject createChatContext(String chatId) {
        if (StringUtils.isBlank(chatId)) {
            OkHttpClient client = new OkHttpClient();
            Request request = getAuthHeader()
                    .url("https://moss.fastnlp.top/api/chats")
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                JSONObject jsonResponse = JSONObject.parseObject(response.body().string());
                chatId = jsonResponse.getString("id");
            } catch (IOException e) {
                log.error("Error creating conversation: " + e.getMessage());
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("chatId", chatId != null ? chatId : "0");
        return jsonObject;
    }

    public boolean checkAvailability() {
        if (access_token == null || access_token.isEmpty()) {
            return false;
        }
        OkHttpClient client = new OkHttpClient();
        Request request = getAuthHeader()
                .url("https://moss.fastnlp.top/api/users/me")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
