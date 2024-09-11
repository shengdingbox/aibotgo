package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class PiLLMService extends AbstractLLMService {

    private static final ReentrantLock lock = new ReentrantLock();

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    ;

    public PiLLMService() {
        BASE_URL = "https://pi.ai/";
        MODEL_NAME = AdiConstant.ModelPlatform.PI;
    }

    @Override
    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        lock.lock();
        String chatId = "";
        try {
            String payload = String.format("{\"text\":\"%s\"}", assistantChatParams.getMessageId());

            Request request = new Request.Builder()
                    .url("https://pi.ai/api/chat")
                    .post(RequestBody.create(payload, MediaType.parse("application/json")))
                    .addHeader("accept", "text/event-stream")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Api-Version", "3")
                    .build();

            EventSourceListener listener = new EventSourceListener() {
                StringBuilder text = new StringBuilder();

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if (type.equals("partial")) {
                        String parsedText = parsePartialData(data);
                        text.append(parsedText);
                        onUpdateResponse.accept(callbackParam, createResponse(chatId, parsedText, false));
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    onUpdateResponse.accept(callbackParam, createResponse(chatId, text.toString(), true));
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
        return null;
    }

    /**
     * Check whether the bot is available. Always returns true for PiBot.
     *
     * @return true
     */
    public boolean checkAvailability() {
        return true;
    }


    /**
     * A simple parser for the partial event data. This function should be modified
     * to fit the actual structure of the partial data.
     *
     * @param data the raw JSON data from the SSE event
     * @return the extracted text content
     */
    private String parsePartialData(String data) {
        // Assuming the data is in JSON format and contains a "text" field
        try {
            // A basic parser for the JSON response (replace with a proper JSON library if needed)
            String text = data.replaceAll(".*\"text\":\"(.*?)\".*", "$1");
            return text;
        } catch (Exception e) {
            System.err.println("Error parsing SSE data: " + e.getMessage());
            return "";
        }
    }
}
