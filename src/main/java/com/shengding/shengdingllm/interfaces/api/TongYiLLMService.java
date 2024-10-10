package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.interfaces.EventSourceStreamListener;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import static com.shengding.shengdingllm.cosntant.AdiConstant.CHAT_IO;
@Slf4j
@Service
public class TongYiLLMService extends AbstractLLMService {

    private static final ReentrantLock lock = new ReentrantLock();

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public TongYiLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.TONGYI;
        BASE_URL = "https://qianwen.aliyun.com";
    }

    private Map<String, String> getRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-xsrf-token", getStoreXsrfToken());  // Replace getStoreXsrfToken() with your actual implementation
        headers.put("cookie", access_token);  // Replace getStoreXsrfToken() with your actual implementation
        return headers;
    }

    private String getStoreXsrfToken() {
        // Retrieve the XSRF token from your store or session management
        return "7c23cd94-7f83-4ff9-889c-36a6a6e1990b";
    }

    @Override
    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        lock.lock();
        try {
            JSONObject context = createChatContext(assistantChatParams.getMessageId());
//            if (context.containsKey("exception")) {
//                throw new RuntimeException(context.get("exception"));
//            }
            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("action", "next");
//            jsonObject.put("parentMsgId", assistantChatParams.getMessageId());
//            jsonObject.put("requestId",cccc);
//            jsonObject.put("sessionId", context.getString("sessionId"));
//            jsonObject.put("sessionType", "text_chat");
//            jsonObject.put("userAction", "chat");
//            jsonObject.put("mode", "chat");
//            jsonObject.put("model", "");
//            JSONObject params = new JSONObject();
//            params.put("agentId", "");
//            params.put("searchType", assistantChatParams.);
//            params.put("pptGenerate", false);
//            jsonObject.put("params", params);
//            JSONObject content = JSONObject.
//            List<Message> messages = assistantChatParams.getMessages();

            jsonObject.put("contents", assistantChatParams.getUserMessage().getContent());


            String chatId = context.getString(CHAT_IO);
            String payload = String.format(""
                    ,
                    generateRandomId(),
                    context.getOrDefault("parentMsgId", "0"),
                    assistantChatParams.getUserMessage().getContent(),
                    chatId
            );

            Request request = new Request.Builder()
                    .url("https://qianwen.aliyun.com/conversation")
                    .post(RequestBody.create(payload, MediaType.parse("application/json")))
                    .headers(Headers.of(getRequestHeaders()))
                    .addHeader("accept", "text/event-stream")
                    .addHeader("content-type", "application/json")
                    .build();

            EventSourceListener listener = new EventSourceStreamListener(client,request) {
                StringBuilder responseContent = new StringBuilder();

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if ("[DONE]".equals(data)) return;

                    if ("".equals(data)) {
                        // Handle empty response (indicating an error)
                        log.error("Received empty response");
                    } else {
                        responseContent.append(data);
                        onUpdateResponse.accept(callbackParam, createResponse("", data, false));
                        if ("stop".equals(type)) {
                            context.put("parentMsgId", data); // Assuming data contains msgId
                            onUpdateResponse.accept(callbackParam, createResponse(chatId, responseContent.toString(), true));
                        }
                    }
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    log.error("Error during SSE connection: " + t.getMessage());
                    onUpdateResponse.accept(callbackParam,createResponse(chatId,"Error during SSE connection", true));
                }
            };

            EventSources.createFactory(client).newEventSource(request, listener);

        } catch (Exception e) {
            log.error("Error in sendPrompt: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected JSONObject createChatContext(String chatId) {
        JSONObject context = new JSONObject();
        if(StringUtils.isBlank(chatId)){
            Request request = new Request.Builder()
                    .url("https://qianwen.aliyun.com/addSession")
                    .post(RequestBody.create("{\"firstQuery\":\"ChatALL\",\"sessionType\":\"text_chat\"}", MediaType.parse("application/json")))
                    .headers(Headers.of(getRequestHeaders()))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    // Assuming the response has fields "sessionId", "userId", and "parentMsgId"
                    // Populate context with these values
                    context.put("sessionId", "extracted-session-id");
                    context.put("parentMsgId", "0");
                } else {
                    log.error("Error QianWen adding session: " + response.message());
                    context.put("exception", response.message());
                }
            } catch (IOException e) {
                log.error("Error QianWen adding session: " + e.getMessage());
                context.put("exception", e.getMessage());
            }
        }
        return context;
    }

    public boolean checkAvailability() {
        boolean available = false;
        Request request = new Request.Builder()
                .url("https://qianwen.aliyun.com/querySign")
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .headers(Headers.of(getRequestHeaders()))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                available = responseBody.contains("\"success\":true");
            } else {
                log.error("Error QianWen check login: " + response.message());
            }
        } catch (IOException e) {
            log.error("Error QianWen check login: " + e.getMessage());
        }

        return available;
    }

    private String generateRandomId() {
        StringBuilder randomStr = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            randomStr.append(Integer.toHexString((int) (Math.random() * 16)));
        }
        return randomStr.toString();
    }
}
