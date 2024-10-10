package com.shengding.shengdingllm.interfaces.api;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import io.micrometer.common.lang.Nullable;
import io.micrometer.common.util.StringUtils;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * OpenAi LLM service
 */
@Slf4j
@Accessors(chain = true)
public class XingHuoLLMService extends AbstractLLMService {

    public XingHuoLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.XUNFEI;
        BASE_URL = "https://xinghuo.xfyun.cn";
    }

    @Override
    public boolean checkAvailability() {
        boolean available = false;
        try {
            String response = HttpUtil.get(BASE_URL + "/iflygpt/userInfo");
            Map<String, Object> responseMap = JSONUtil.toBean(response, Map.class);
            available = MapUtil.getBool(responseMap, "flag", false);
        } catch (Exception e) {
            log.error("Error checking Spark login status: " + e.getMessage());
        }
        return available;
    }

    @Override
    public JSONObject createChatContext(String chatId) {
        JSONObject jsonObject = new JSONObject();
        if (StringUtils.isBlank(chatId)) {
            try {
                String response = HttpRequest.post(BASE_URL+"/iflygpt/u/chat-list/v1/create-chat-list")
                        .cookie(access_token)
                        .contentType("application/json")
                        .body("{}")
                        .execute().body();
                Map<String, Object> responseMap = JSONUtil.toBean(response, Map.class);
                if (MapUtil.getBool(responseMap, "flag") && MapUtil.getInt(responseMap, "code", -1) == 0) {
                    chatId = MapUtil.getStr((Map<String, Object>) responseMap.get("data"), "id");
                } else {
                    log.error("Error creating conversation: " + responseMap.get("desc"));
                }
            } catch (Exception e) {
                log.error("Error creating conversation: " + e.getMessage());
            }
        }
        jsonObject.put("chatId", chatId != null ? chatId : "0");
        return jsonObject;
    }

    @Override
    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(50, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.MINUTES)
                .hostnameVerifier((hostname, session) -> true)
                .build();

        EventSource.Factory factory = EventSources.createFactory(client);
        Map<String, Object> context = createChatContext(assistantChatParams.getMessageId());
        String chatId = String.valueOf(context.get("chatId"));
        FormBody formBody = new FormBody.Builder().add("fd", String.valueOf(System.currentTimeMillis() % 1000000))
                .add("chatId", chatId)
                .add("text", assistantChatParams.getUserMessage().getContent())
                .add("GtToken", access_token)
                .add("clientType", "1")
                .add("isBot", "0").build();
        // 请求对象
        Request request = new Request.Builder()
                .url(BASE_URL+"/iflygpt-chat/u/chat_message/chat")
                .header("Cookie", access_token)
                .post(formBody)
                .build();

        // 自定义监听器
        final String[] text = {""};
        EventSourceListener eventSourceListener = new EventSourceListener() {
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
        };

        factory.newEventSource(request, eventSourceListener);
    }
}
