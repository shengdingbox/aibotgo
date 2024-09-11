package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import static com.shengding.shengdingllm.cosntant.AdiConstant.CHAT_IO;

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
        headers.put("cookie", "login_current_pk=1478362499300252; yunpk=1478362499300252; cna=3ssIHVy1eSkBASQJigCvIIkj; cnaui=1478362499300252; aui=1478362499300252; ssxmod_itna=QqjxnD0D2i=xyDlRA+fv8GzG8BqDCDoi+KDmxhu4GNpaDZDiqAPGhDC3+9DjQZe5eGz0bBGYxogRi4F3rBSeKFrdax0aDbqGkAbF4iiBDCeDIDWeDiDG4tDFxYoDePNQDFWqvU1cmxWKDKx0kDY5DwZv8DYPDWxDFfExo6hDDBhiIYee4KDi3f=vFP/yhDiyDuEG4fxG1DQ5Ds2rjYAKD0od6YmyCEUfvji9Gq40OD0I+22chDBbkMaH7zImxNOieVDbeCBRD3D4Dgmix7uDPeBTBhT0eHQiBQQD5hkre6Hx4DDp4d7rPNiDD===; ssxmod_itna2=QqjxnD0D2i=xyDlRA+fv8GzG8BqDCDoi+KDmxhqA6aGCQDBMmRP7PPwxQlTWUK3+E0KGFtOM2Qweabl7RhKrfnG2x9YeqkrL3gb=Ic+C+FICW4KZUjknkPsl9ACpv2SehrcgE5azrlo9WIOWO9O7WwhHk=oPW8aerSED5RYot9DbQWfgw7E0IN2AziEWEha3kNp3hvzaWfKPIPr3TOPq65IMTjrdkAtdbAED4CUFjUzQWRWf6cIjEdXLjtxLciw6X3m4bCTDY9CRqGlGxD6CB3rL8HRaAu/UwPxyo43E8ctX40E2j3leKYQvEvcaEEAI=oEu7NCoEboiqz5c2L27EOcX0YoBlDjeETQQRgE8PR0Z2za0C1tSjLUeUB0AUeRWzmhh+BxYtA2x0paUU21P=4iIYnu0XVxrspNBpwCZt8tjh8iwir81x5KcSBnEiuDbePRi+eEpFcpKTUCNwbuuAaAP5hhjc2Pc64DQFp0Gim3Ylb4i+5Kbxgi1KGdYRG=9ixY=bbY4vt9mVCx4AbhrRxnRDYRmLjdA+Q3I=C2Y8jYnePecIo15=154CbzGyQDxBFNA2q0=Aob2lHtYDY6hKK0Lhh8lZoUhZC2i9lj22jrUA1xDFqD+OjPnxqjlP7ou2oKD+xNmqDbthhRrzCM4xD==; tongyi_guest_ticket=rjDbTaRduufXj6bDdW*gmWSfubRu6BQ53c2yDcQuUoyW8m8__$Ed4uyDmebPMET7b26G7gDrxSUx0; tongyi_sso_ticket=ZKWPsSFP2re8U*tntMAHELF6sFvJ_vW6hGe4v3Ozg22ecdDC9dlYxuK6ugFMPUFC0; t=d51f129cd0b0f947e32c9056a59a543c; channel=rZsgMXervlmWpmDQsQ8nCQXF07NxAw7IvAGsAb82hl20RGgyeI%2FZxRHL0xvULG3qICjL%2FBIBgHpXN7DgXnWcOw%3D%3D; currentRegionId=cn-hangzhou; aliyun_country=CN; aliyun_site=CN; aliyun_lang=zh; sca=4c08b413; atpsida=57d35f14f7e8aaab034a0e69_1723904777_1; isg=BO3tvrcud4yoSBGTaajhhBKn_IlnSiEcgFI7Oy_wfwT4pghY4Z1C7a0wkHpAJjnU; tfstk=cDWGB2wy01R_bRiLxl9_MZyFZKScadcwyt5PLPSgODdHkzXwbsDxTbzX9kxYPPlf.");  // Replace getStoreXsrfToken() with your actual implementation
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

            String chatId = context.getString(CHAT_IO);
            String payload = String.format(
                    "{\"action\":\"next\",\"msgId\":\"%s\",\"parentMsgId\":\"%s\",\"contents\":[{\"contentType\":\"text\",\"content\":\"%s\"}],\"timeout\":17,\"openSearch\":false,\"sessionId\":\"%s\",\"model\":\"\"}",
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
                        System.err.println("Received empty response");
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
                    System.err.println("Error during SSE connection: " + t.getMessage());
                    onUpdateResponse.accept(callbackParam,createResponse(chatId,"Error during SSE connection", true));
                }
            };

            EventSources.createFactory(client).newEventSource(request, listener);

        } catch (Exception e) {
            System.err.println("Error in sendPrompt: " + e.getMessage());
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
                    System.err.println("Error QianWen adding session: " + response.message());
                    context.put("exception", response.message());
                }
            } catch (IOException e) {
                System.err.println("Error QianWen adding session: " + e.getMessage());
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
                System.err.println("Error QianWen check login: " + response.message());
            }
        } catch (IOException e) {
            System.err.println("Error QianWen check login: " + e.getMessage());
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
