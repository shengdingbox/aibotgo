//package com.shengding.shengdingllm.interfaces.api;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.shengding.shengdingllm.interfaces.AbstractLLMService;
//import com.shengding.shengdingllm.vo.AssistantChatParams;
//import okhttp3.*;
//import okhttp3.sse.EventSource;
//import okhttp3.sse.EventSourceListener;
//import okhttp3.sse.EventSources;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.function.BiConsumer;
//
//public class PhindLLMService extends AbstractLLMService {
//
//
//    private static final String LOGIN_URL = "https://www.phind.com";
//
//    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
//            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
//            .build();;
//
//    @Override
//    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
//        ChatContext context = createChatContext();
//        String rewriteUrl = "https://www.phind.com/api/infer/followup/rewrite";
//        String searchUrl = "https://www.phind.com/api/web/search";
//        String answerUrl = "https://www.phind.com/api/infer/answer";
//
//        String prompt = assistantChatParams.getUserMessage().getContent();
//        JSONObject rewriteResponse = performPostRequest(rewriteUrl, createRewritePayload(prompt, context));
//        JSONObject searchResponse = performPostRequest(searchUrl, createSearchPayload(rewriteResponse.getString("query")));
//
//        String payload = createAnswerPayload(prompt, context, searchResponse).toString();
//
//        Request request = new Request.Builder()
//                .url(answerUrl)
//                .post(RequestBody.create(payload, MediaType.parse("application/json")))
//                .addHeader("accept", "text/event-stream")
//                .addHeader("Content-Type", "application/json")
//                .build();
//
//        new EventSourceStreamListener(okHttpClient,request){
//
//            boolean isSuccess = false;
//            @Override
//            public void onEvent(EventSource eventSource, String id, String type, String eventData) {
//                if (eventData != null) {
//                    if (eventData.startsWith("<PHIND_METADATA>")) {
//                        isSuccess = true;
//                    } else {
//                        text.append(data);
//                        onUpdateResponse.accept(text.toString(), false);
//                    }
//                }
//            }
//        });
//        EventSourceListener listener = new EventSourceListener() {
//            StringBuilder text = new StringBuilder();
//            boolean isSuccess = false;
//
//            @Override
//            public void onEvent(EventSource eventSource, String id, String type, String data) {
//                if (data != null) {
//                    if (data.startsWith("<PHIND_METADATA>")) {
//                        isSuccess = true;
//                    } else {
//                        text.append(data);
//                        onUpdateResponse.accept(text.toString(), false);
//                    }
//                }
//            }
//
//            @Override
//            public void onClosed(EventSource eventSource) {
//                if (isSuccess) {
//                    updateChatContext(context, prompt, text.toString(), searchResponse);
//                }
//                onUpdateResponse.accept(text.toString(), true);
//            }
//
//            @Override
//            public void onFailure(EventSource eventSource, Throwable t, Response response) {
//                System.err.println("Error during SSE connection: " + t.getMessage());
//                onUpdateResponse.accept("Error during SSE connection", true);
//            }
//        };
//
//        EventSources.createFactory(client).newEventSource(request, listener);
//    }
//
//    @Override
//    protected Map<String, Object> createChatContext(String chatId) {
//        return null;
//    }
//
//    public boolean checkAvailability() {
//        return true;
//    }
//
//    public void sendPrompt(String prompt, BiConsumer<String, Boolean> onUpdateResponse, Object callbackParam) {
//
//    }
//
//    private JSONObject createRewritePayload(String prompt, ChatContext context) {
//        JSONObject payload = new JSONObject();
//        payload.put("questionToRewrite", prompt);
//        payload.put("questionHistory", new JSONArray(context.questionHistory));
//        payload.put("answerHistory", new JSONArray(context.answerHistory));
//        return payload;
//    }
//
//    private JSONObject createSearchPayload(String query) {
//        JSONObject payload = new JSONObject();
//        payload.put("q", query);
//        payload.put("browserLanguage", "en-GB");
//        payload.put("userSearchRules", new JSONObject());
//        return payload;
//    }
//
//    private JSONObject createAnswerPayload(String prompt, ChatContext context, JSONObject searchResponse) {
//        JSONObject payload = new JSONObject();
//        payload.put("questionHistory", new JSONArray(context.questionHistory));
//        payload.put("answerHistory", new JSONArray(context.answerHistory));
//        payload.put("question", prompt);
//        payload.put("webResults", searchResponse);
//        payload.put("options", createOptions());
//        payload.put("context", "");
//        return payload;
//    }
//
//    private JSONObject createOptions() {
//        JSONObject options = new JSONObject();
//        options.put("date", getFormattedDate());
//        options.put("language", "en-GB");
//        options.put("detailed", true);
//        options.put("anonUserId", getUUID());
//        options.put("answerModel", "default");
//        options.put("customLinks", new JSONArray());
//        return options;
//    }
//
//    private String getFormattedDate() {
//        java.time.LocalDate date = java.time.LocalDate.now();
//        return date.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
//    }
//
//    private String getUUID() {
//        // Replace with actual logic to get UUID
//        return java.util.UUID.randomUUID().toString();
//    }
//
//    private JSONObject performPostRequest(String url, JSONObject payload) {
//        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
//        Request request = new Request.Builder()
//                .url(url)
//                .post(body)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return JSONObject.parseObject(response.body().string());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void updateChatContext(ChatContext context, String prompt, String text, JSONObject searchResponse) {
//        context.answerHistory.add(text);
//        context.questionHistory.add(prompt);
//
//        // Replace links with hostnames
//        JSONArray searchResults = searchResponse.getJSONArray("data");
//        for (int i = 0; i < searchResults.size(); i++) {
//            String url = searchResults.getJSONObject(i).getString("url");
//            String hostname = java.net.URI.create(url).getHost();
//            text = text.replace("[Source" + i + "]", "[" + hostname + "]");
//            text = text.replace("[^" + i + "^]", " [" + hostname + "](" + url + ")");
//            text = text.replace("^" + i + "^", " [" + hostname + "](" + url + ")");
//        }
//    }
//
//    private ChatContext createChatContext() {
//        return new ChatContext(new ArrayList<>(), new ArrayList<>());
//    }
//
//    private static class ChatContext {
//        List<String> questionHistory;
//        List<String> answerHistory;
//
//        ChatContext(List<String> questionHistory, List<String> answerHistory) {
//            this.questionHistory = questionHistory;
//            this.answerHistory = answerHistory;
//        }
//    }
//
//    public static void main(String[] args) {
//        new PhindLLMService().sendPrompt("你好?", (text, isDone) -> {
//            System.out.println("Response: " + text);
//            System.out.println("Done: " + isDone);
//        }, null);
//    }
//}
