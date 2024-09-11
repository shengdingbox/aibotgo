package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.api.request.Message;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.utils.ResponseManager;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import io.micrometer.common.lang.Nullable;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Service
public class KimiLLMService extends AbstractLLMService {

    public KimiLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.KIMI;
        BASE_URL = "https://kimi.moonshot.cn";
    }

    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(50, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .hostnameVerifier((hostname, session) -> true)
            .build();

    private Map<String, String> getAuthHeader() {
        Map<String, String> pairs = new HashMap<>();
        pairs.put("accept", "application/json, text/plain, */*");
        pairs.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,zh-HK;q=0.5,zh-TW;q=0.4");
        pairs.put("authorization", "Bearer " + access_token);
        pairs.put("cache-control", "no-cache");
        pairs.put("cookie", "Hm_lvt_358cae4815e85d48f7e8ab7f3680a74b=1725434543; Hm_lpvt_358cae4815e85d48f7e8ab7f3680a74b=17254 34544; HMACCOUNT=6DA7B554009CDBE3; _ga=GA1.1.1942321256.1725434544; _gcl_au=1.1.1730639990.1725434544; _ga_YXD8W70SZP=GS1.1.1725441294.2.0.1725441294.0.0.0");
        pairs.put("pragma", "no-cache");
        pairs.put("priority", "u=1, i");
        pairs.put("referer", "https://kimi.moonshot.cn/chat/crc2a3atnn0k573sij8g");
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

    @Override
    public boolean checkAvailability() {
        boolean available = false;
        try {
            refreshTokens();
            available = true;
        } catch (IOException e) {
            available = false;
            System.err.println("Error checking Kimi login status: " + e.getMessage());
        }
        return available;
    }

    private void refreshTokens() throws IOException {
        String refreshUrl = BASE_URL + "/api/auth/token/refresh";
        Request.Builder url = new Request.Builder()
                .url(refreshUrl);
        Map<String, String> authHeader = getAuthHeader();
        authHeader.forEach(url::addHeader);
        Request request = url.build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                byte[] bytes = responseBody.getBytes("GBK");
                String decodedBody = new String(bytes, StandardCharsets.UTF_8);
                JSONObject jsonResponse = JSONObject.parseObject(decodedBody);
                access_token = jsonResponse.getString("access_token");
                //refresh_token = jsonResponse.getString("refresh_token");
            }
        }
    }

    @Override
    public void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        EventSource.Factory factory = EventSources.createFactory(client);
        JSONObject context = createChatContext(assistantChatParams.getMessageId());
        JSONObject jsonObject = new JSONObject();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", assistantChatParams.getUserMessage().getContent());
        JSONArray objects = new JSONArray();
        objects.add(message);
        jsonObject.put("messages", objects);
        jsonObject.put("refs", new JSONArray());
        jsonObject.put("use_search", true);
        jsonObject.put("kimiplus_id", "kimi");
        JSONObject extend = new JSONObject();
        extend.put("sidebar", true);
        jsonObject.put("extend", extend);
        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));

        final String chatId = context.getString("chatId");
        Request.Builder post = new Request.Builder()
                .url(BASE_URL + "/api/chat/" + chatId + "/completion/stream")
                .post(body);
        getAuthHeader().forEach(post::addHeader);
        Request request = post
                .build();

        // 自定义监听器
        final StringBuffer beginning = new StringBuffer();
        EventSourceListener eventSourceListener = new EventSourceListener() {

            @Override
            public void onOpen(EventSource eventSource, Response response) {
                super.onOpen(eventSource, response);
            }

            @Override
            public void onEvent(EventSource eventSource, @Nullable String id, @Nullable String type, String eventData) {
                JSONObject jsonData = JSONObject.parseObject(eventData);
                if ("search_plus".equals(jsonData.getString("event"))) {
                    String msgType = jsonData.getString("msg.type");
                    if ("start_res".equals(msgType)) {
                        beginning.append("> 搜索中...\n");
                    } else if ("get_res".equals(msgType)) {
                        beginning.append("> 找到 ")
                                .append(jsonData.getInteger("msg.successNum"))
                                .append(" 结果 [")
                                .append(jsonData.getString("msg.title"))
                                .append("](")
                                .append(jsonData.getString("msg.url"))
                                .append(")\n").toString();
                    }
                } else if ("cmpl".equals(jsonData.getString("event"))) {
                    String string = jsonData.getString("text");
                    beginning.append(string);
                    System.err.println(beginning);
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, string, false));
                } else if ("all_done".equals(jsonData.getString("event"))) {
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, beginning.toString(), true));
                } else {
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, beginning.toString(), false));
                }
                super.onEvent(eventSource, id, type, eventData);
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                if (t != null) {
                    System.err.println("Error occurred: " + t.getMessage());
                }
                if (response != null) {
                    System.err.println("Response code: " + response.code() + response.body());
                }
                super.onFailure(eventSource, t, response);
            }

            @Override
            public void onClosed(EventSource eventSource) {
                super.onClosed(eventSource);
            }
        };
        factory.newEventSource(request, eventSourceListener);
    }

    @Override
    public JSONObject createChatContext(String messageId) {
        JSONObject context = new JSONObject();
        checkAvailability();
        if (StringUtils.isBlank(messageId)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("is_example", false);
            jsonObject.put("name", "ChatALL");
            Request.Builder post = new Request.Builder()
                    .url(BASE_URL + "/api/chat")
                    .post(RequestBody.create(jsonObject.toString(), MediaType.parse("application/json")));
            getAuthHeader().forEach(post::addHeader);
            Request request = post.build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject jsonResponse = JSONObject.parseObject(response.body().string());
                    messageId = jsonResponse.getString("id");
                } else {
                    System.err.println("Error checking Spark login status: " + response.message());
                    throw new RuntimeException("Error creating conversation: " + response.message());
                }
            } catch (IOException e) {
                System.err.println("Error creating conversation: " + e.getMessage());
            }
        }
        context.put("chatId", messageId);
        return context;
    }

    public static void main(String[] args) {
        com.shengding.shengdingllm.utils.ResponseManager manager = new ResponseManager();
        String finalResponse = null;
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", Integer.toString(8888));
        Message message = new Message();
        message.setContent("你好");
        AssistantChatParams assistantChatParams = AssistantChatParams.builder()
                .userMessage(message).build();
        new KimiLLMService().sendPrompt(assistantChatParams, manager::handleUpdate, manager);
        try {
            finalResponse = manager.getResponse();
            System.out.println(finalResponse);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
