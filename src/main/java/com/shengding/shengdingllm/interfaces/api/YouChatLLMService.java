package com.shengding.shengdingllm.interfaces.api;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.api.request.Message;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.utils.ResponseManager;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import io.micrometer.common.lang.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.apache.commons.lang.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class YouChatLLMService extends AbstractLLMService {
    private static final ReentrantLock lock = new ReentrantLock();


    public YouChatLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.YOUCHAT;
        BASE_URL = "https://you.com/";
    }

    @Override
    public boolean checkAvailability() {
        // In this case, we are just returning true
        return true;
    }

    @Override
    public void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(50, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.MINUTES)
                .hostnameVerifier((hostname, session) -> true)
                .build();

        EventSource.Factory factory = EventSources.createFactory(client);
        Map<String, Object> context = createChatContext(assistantChatParams.getMessageId());
        String chatId = String.valueOf(context.get("chatId"));
        List<Map<String, String>> chatHistory = (List<Map<String, String>>) context.get("chatHistory");
        String payload = "q=" + urlEncode(assistantChatParams.getUserMessage().getContent()) + "&domain=youchat&chatId=" + urlEncode(chatId)
                + "&queryTraceId=" + urlEncode(chatId) + "&chat=" + urlEncode(chatHistory.toString());
        // 请求对象
        Request request = new Request.Builder()
                .url("https://you.com/api/streamingSearch?" + payload)
                .header("Accept", "text/event-stream")
                .get()
                .build();

        // 自定义监听器
        final String[] text = {""};
        EventSourceListener eventSourceListener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                super.onOpen(eventSource, response);
            }

            @Override
            public void onEvent(EventSource eventSource, @Nullable String id, @Nullable String type, String eventData) {
                //   接受消息 data
                if ("<end>".equals(eventData)) {
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, text[0], true));
                    return;
                } else if (eventData.endsWith("<sid>")) {
                    return; // Ignore <sid> message
                } else {
                    String partialText;
                    if (eventData.startsWith("{")) {
                        Map<String, Object> data = JSONUtil.toBean(eventData, Map.class);
                        partialText = MapUtil.getStr(data, "descr");
                    } else if (eventData.startsWith("[")) {
                        partialText = eventData; // Error or geeError
                    } else {
                        partialText = Base64.decodeStr(eventData);
                    }
                    text[0] += partialText;
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, partialText, false));
                }
                super.onEvent(eventSource, id, type, eventData);
            }

            @Override
            public void onClosed(EventSource eventSource) {
                super.onClosed(eventSource);
            }

            @Override
            public void onFailure(EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                if (t != null) {
                    System.err.println("Error occurred: " + t.getMessage());
                }
                if (response != null) {
                    System.err.println("Response code: " + response.code());
                }
                super.onFailure(eventSource, t, response);
            }
        };

        factory.newEventSource(request, eventSourceListener);

    }

    @Override
    public JSONObject createChatContext(String chatId) {
        JSONObject context = new JSONObject();
        if(StringUtils.isEmpty(chatId)) {
            chatId = UUID.randomUUID().toString();
        }
        context.put("chatId", chatId);
        context.put("chatHistory", new ArrayList<Map<String, String>>());
        return context;
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public static void main(String[] args) {
        ResponseManager manager = new ResponseManager();
        String finalResponse = null;
        Message message = new Message();
        message.setContent("你好");
        AssistantChatParams assistantChatParams = AssistantChatParams.builder()
                .userMessage(message).build();
        new YouChatLLMService().sendPrompt(assistantChatParams, manager::handleUpdate, manager);
        try {
            finalResponse = manager.getResponse();
            System.out.println(finalResponse);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
