package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.api.request.Message;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.interfaces.EventSourceStreamListener;
import com.shengding.shengdingllm.utils.ResponseManager;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import io.micrometer.common.lang.Nullable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Service
@Slf4j
public class MiniMaxLLMService extends AbstractLLMService {

    public MiniMaxLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.MINIMAX;
        BASE_URL = "https://hailuoai.com";
    }

    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(50, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .hostnameVerifier((hostname, session) -> true)
            .build();

    private Map<String, String> getAuthHeader() {
        Map<String, String> pairs = new HashMap<>();
        pairs.put("accept", "multipart/form-data; boundary=----WebKitFormBoundary0gzBRpG9l3Y8GXrB");
       // pairs.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,zh-HK;q=0.5,zh-TW;q=0.4");
        pairs.put("token", access_token);
//        pairs.put("cache-control", "no-cache");
//        pairs.put("pragma", "no-cache");
//        pairs.put("priority", "u=1, i");
//        pairs.put("referer", "https://hailuoai.com/?type=chat&chatID=291291257549438978");
//        pairs.put("sec-ch-ua", "Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128");
//        pairs.put("sec-ch-ua-mobile", "?0");
//        pairs.put("sec-ch-ua-platform", "macOS");
//        pairs.put("sec-fetch-dest", "empty");
//        pairs.put("sec-fetch-mode", "cors");
//        pairs.put("upgrade-insecure-requests", "1");
//        pairs.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
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
            log.error("Error checking Kimi login status: " + e.getMessage());
        }
        return available;
    }

    @Override
    public void sendMessage(String phone) {

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
        //JSONObject context = createChatContext(assistantChatParams.getMessageId());
        final String chatId = "0";
        RequestBody body = FormBody.create("characterID=1&msgContent="+assistantChatParams.getUserMessage().getContent()+"&chatID="+chatId+"&searchMode=0",MediaType.parse("multipart/form-data"));
        Request.Builder post = new Request.Builder()
                .url("https://hailuoai.com/v4/api/chat/msg?device_platform=web&app_id=3001&version_code=22201&uuid=9ace8e02-6d79-45ff-83ac-32ea5163dabf&device_id=291289704640602121&os_name=Mac&browser_name=chrome&device_memory=8&cpu_core_num=6&browser_language=zh-CN&browser_platform=MacIntel&screen_width=1680&screen_height=1050&unix=1726305431000")
                .post(body);
        getAuthHeader().forEach(post::addHeader);
        Request request = post
                .build();

        // 自定义监听器
        final StringBuffer beginning = new StringBuffer();
        new EventSourceStreamListener(client, request) {
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
                    log.error(String.valueOf(beginning));
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, string, false));
                } else if ("all_done".equals(jsonData.getString("event"))) {
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, beginning.toString(), true));
                } else {
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, beginning.toString(), false));
                }
            }
        };
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
                    log.error("Error checking Spark login status: " + response.message());
                    throw new RuntimeException("Error creating conversation: " + response.message());
                }
            } catch (IOException e) {
                log.error("Error creating conversation: " + e.getMessage());
            }
        }
        context.put("chatId", messageId);
        return context;
    }

    public static void main(String[] args) {
        ResponseManager manager = new ResponseManager();
        String finalResponse = null;
        Message message = new Message();
        message.setContent("你好");
        AssistantChatParams assistantChatParams = AssistantChatParams.builder()
                .userMessage(message).build();
        MiniMaxLLMService miniMaxLLMService = new MiniMaxLLMService();
        miniMaxLLMService.setAccess_token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3Mjk3NjEzODIsInVzZXIiOnsiaWQiOiIyNzQ2MjQ4NjI1NTA3NDA5OTgiLCJuYW1lIjoi5bCP6J665bi9OTk4IiwiYXZhdGFyIjoiaHR0cHM6Ly9jZG4ueWluZ3NoaS1haS5jb20vcHJvZC91c2VyX2F2YXRhci8xNzA2MjY3NTQ0Mzg5ODIwODAxLTE3MzE5NDU3MDY2ODk2NTg5Nm92ZXJzaXplLnBuZyIsImRldmljZUlEIjoiMjkxMjg5NzA0NjQwNjAyMTIxIiwiaXNBbm9ueW1vdXMiOmZhbHNlfX0.QYVLDB4ehJlaXxhPqPuaVXQKWFRsOtE6uDWQrj7fH7A");
        miniMaxLLMService.sendPrompt(assistantChatParams, manager::handleUpdate, manager);
        try {
            finalResponse = manager.getResponse();
            log.info(finalResponse);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
