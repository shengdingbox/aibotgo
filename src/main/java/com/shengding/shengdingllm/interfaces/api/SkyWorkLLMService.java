package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.api.request.Message;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.shengding.shengdingllm.cosntant.AdiConstant.CHAT_IO;
@Slf4j
@Service
public class SkyWorkLLMService extends AbstractLLMService {

    public SkyWorkLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.SKYWORK;
        BASE_URL = "https://chat.tiangong.cn";
    }

    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private Headers getAuthHeader() {
        // 假设inviteToken和token已经在其他地方获取并存储，可以通过某种方式获取
        String inviteToken = "Bearer " + access_token;
        String token = "Bearer " + access_token;
        return new Headers.Builder()
                .add("invite-token", inviteToken)
                .add("token", token)
                .build();
    }

    @Override
    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        try {
            JSONObject context = createChatContext(assistantChatParams.getMessageId());
            if (context == null || context.get("session_id") == null) {
                checkAvailability();
                context = createChatContext(assistantChatParams.getMessageId());
            }

            String uuid = UUID.randomUUID().toString();
            String streamContextUrl = BASE_URL + "/api/v1/chat/chat?__requestid=" + uuid;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("content", assistantChatParams.getUserMessage().getContent());
            jsonObject.put("session_id", context.get("session_id"));

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toJSONString());
            Request request = new Request.Builder()
                    .url(streamContextUrl)
                    .post(requestBody)
                    .headers(getAuthHeader())
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject data = JSON.parseObject(responseBody);
                    String messageId = data.getJSONObject("resp_data").getJSONObject("result_message").getString("message_id");

//                    super.url = "https://api-chat.tiangong.cn/api/v1/chat/getMessage?message_id=" + messageId;
//                    super.headers = getAuthHeader();
//                    super.onUpdateResponse = onUpdateResponse;
//                    super.callbackParam = callbackParam;
//                    super.startStream();
                } else {
                    throw new IOException("Failed to get stream context: " + response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected JSONObject createChatContext(String chatId) {
        JSONObject json = new JSONObject();
        if (StringUtils.isBlank(chatId)) {
            String url = BASE_URL + "/api/v1/session/newSession";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                    .headers(getAuthHeader())
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject data = JSON.parseObject(responseBody);
                    json.put("session_id", data.getJSONObject("resp_data").getString("session_id"));
                }
            } catch (Exception e) {
                log.error("Error creating SkyWorkBot context: " + e.getMessage());
            }
        }
        json.put(CHAT_IO, chatId);
        return json;
    }

    public boolean checkAvailability() {
        boolean available = false;
        String verifyUrl = BASE_URL + "/api/v1/user/inviteVerify";
        Request request = new Request.Builder()
                .url(verifyUrl)
                .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                .headers(getAuthHeader())
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JSONObject data = JSON.parseObject(responseBody);
                available = data.getIntValue("code") == 200;

                // 如果邀请令牌过期，可以重新获取
                if (!available && data.getIntValue("code") >= 60100) {
                    String newTokenUrl = BASE_URL + "/api/v1/queue/waitAccess";
                    Request newTokenRequest = new Request.Builder()
                            .url(newTokenUrl)
                            .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                            .headers(getAuthHeader())
                            .build();

                    try (Response newTokenResponse = okHttpClient.newCall(newTokenRequest).execute()) {
                        if (newTokenResponse.isSuccessful() && newTokenResponse.body() != null) {
                            String newTokenResponseBody = newTokenResponse.body().string();
                            JSONObject newTokenData = JSON.parseObject(newTokenResponseBody);
                            available = newTokenData.getIntValue("code") == 200 &&
                                    !newTokenData.getJSONObject("resp_data").getBooleanValue("busy_now");

                            if (available) {
                                // 更新新的inviteToken
                                String newInviteToken = newTokenData.getJSONObject("resp_data").getString("invite_token");
                                // 存储新的inviteToken
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking SkyWorkBot login status: " + e.getMessage());
        }

        return available;
    }


    public static void main(String[] args) {
        AssistantChatParams assistantChatParams = new AssistantChatParams();
        assistantChatParams.setMessages(List.of(
                new Message("你好，SkyWorkBot")
        ));
        new SkyWorkLLMService().sendPrompt(assistantChatParams, (Object o, Map<String, Object> map) -> {
            log.info("{}",map.get("content"));
        }, null);
    }
}
