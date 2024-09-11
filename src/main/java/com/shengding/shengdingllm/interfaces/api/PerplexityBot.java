//package com.shengding.shengdingllm.interfaces.api;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.shengding.shengdingllm.cosntant.AdiConstant;
//import com.shengding.shengdingllm.interfaces.AbstractLLMService;
//import com.shengding.shengdingllm.vo.AssistantChatParams;
//import okhttp3.*;
//import okio.ByteString;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.TimeZone;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//import java.util.function.BiConsumer;
//
//public class PerplexityLLMService extends AbstractLLMService {
//
//    private String lastBackendUUID;
//    private String readWriteToken;
//    private int seq = 1;
//
//    private OkHttpClient client = new OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .build();
//
//    public PerplexityLLMService() {
//        MODEL_NAME = AdiConstant.ModelPlatform.PERPLEXITY;
//        BASE_URL = "https://www.perplexity.ai";
//    }
//
//    @Override
//    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
//        String sid = null;
//
//        try {
//            Request getRequest = new Request.Builder()
//                    .url(BASE_URL + "/socket.io/?EIO=4&transport=polling&t=" + getT())
//                    .get()
//                    .build();
//
//            try (Response response = client.newCall(getRequest).execute()) {
//                if (response.isSuccessful()) {
//                    String responseData = response.body().string();
//                    String[] parts = responseData.split("\\{\"sid\":\"");
//                    if (parts.length > 1) {
//                        sid = parts[1].split("\"")[0];
//                    }
//                }
//            }
//            if (sid != null) {
//                Request postRequest = new Request.Builder()
//                        .url("https://www.perplexity.ai/socket.io/?EIO=4&transport=polling&t=" + getT() + "&sid=" + sid)
//                        .post(RequestBody.create("40{\"jwt\":\"anonymous-ask-user\"}", MediaType.parse("text/plain")))
//                        .build();
//
//                client.newCall(postRequest).execute();
//
//                SSEListener listener = new SSEListener(onUpdateResponse, assistantChatParams.getUserMessage().getContent());
//                client.newWebSocket(new Request.Builder()
//                        .url("wss://www.perplexity.ai/socket.io/?EIO=4&transport=websocket&t=" + getT() + "&sid=" + sid)
//                        .build(), listener);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected JSONObject createChatContext(String chatId) {
//        return null;
//    }
//
//    public boolean checkAvailability() {
//        try {
//            Request request = new Request.Builder()
//                    .url("https://www.perplexity.ai/api/auth/session")
//                    .get()
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                if (response.isSuccessful()) {
//                    String responseData = response.body().string();
//                    JSONObject json = JSONObject.parseObject(responseData);
//                    return json.containsKey("user");
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//
//    private String extractSid(String data) {
//
//        return null;
//    }
//
//    private String getT() {
//        return Z(System.currentTimeMillis());
//    }
//
//    private String Z(long e) {
//        StringBuilder t = new StringBuilder();
//        do {
//            t.insert(0, V[(int) (e % 64)]);
//            e = e / 64;
//        } while (e > 0);
//        return t.toString();
//    }
//
//    private static final char[] V = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_".toCharArray();
//
//    public interface OnUpdateResponseListener {
//        void onUpdate(String content, boolean done);
//    }
//
//    private class SSEListener extends WebSocketListener {
//        private final OnUpdateResponseListener onUpdateResponse;
//        private final String prompt;
//
//        public SSEListener(OnUpdateResponseListener onUpdateResponse, String prompt) {
//            this.onUpdateResponse = onUpdateResponse;
//            this.prompt = prompt;
//        }
//
//        @Override
//        public void onOpen(WebSocket webSocket, Response response) {
//            webSocket.send("2probe");
//        }
//
//        @Override
//        public void onMessage(WebSocket webSocket, String text) {
//            int messageType = Integer.parseInt(text.substring(0, 2));
//            String data = text.substring(2);
//
//            switch (messageType) {
//                case 2:
//                    webSocket.send("3");
//                    break;
//                case 3:
//                    if ("probe".equals(data)) {
//                        JSONArray ask = new JSONArray();
//                        ask.add("perplexity_ask");
//                        ask.add(prompt);
//                        JSONObject options = new JSONObject();
//                        options.put("version", "someVersion"); // Replace with actual version
//                        options.put("source", "default");
//                        options.put("last_backend_uuid", lastBackendUUID);
//                        options.put("read_write_token", readWriteToken);
//                        options.put("attachments", new JSONArray());
//                        options.put("language", "en-US");
//                        options.put("timezone", TimeZone.getDefault().getID());
//                        options.put("search_focus", "internet");
//                        options.put("frontend_uuid", UUID.randomUUID().toString());
//                        options.put("mode", "concise");
//                        options.put("is_related_query", false);
//                        options.put("is_default_related_query", false);
//                        options.put("frontend_context_uuid", UUID.randomUUID().toString());
//                        options.put("prompt_source", "user");
//                        options.put("query_source", lastBackendUUID != null ? "followup" : "home");
//                        ask.add(options);
//
//                        webSocket.send("42" + seq++ + ask.toString());
//                    }
//                    break;
//                case 42:
//                    if (!data.isEmpty()) {
//                        try {
//                            JSONObject result = JSONArray.parseArray(data).getJSONObject(1);
//                            String responseText = result.getString("text");
//                            onUpdateResponse.onUpdate(responseText, false);
//
//                            lastBackendUUID = result.getString("backend_uuid");
//                            readWriteToken = result.getString("read_write_token");
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    break;
//                case 43:
//                    if (!data.isEmpty()) {
//                        try {
//                            JSONObject result = JSONArray.parseArray(data).getJSONObject(0);
//                            String responseText = result.getString("text");
//                            onUpdateResponse.onUpdate(responseText, true);
//
//                            lastBackendUUID = result.getString("backend_uuid");
//                            readWriteToken = result.getString("read_write_token");
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        } finally {
//                            webSocket.close(1000, null);
//                        }
//                    }
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        @Override
//        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
//            t.printStackTrace();
//            onUpdateResponse.onUpdate(null, true);
//        }
//
//        @Override
//        public void onClosing(WebSocket webSocket, int code, String reason) {
//            webSocket.close(1000, null);
//            onUpdateResponse.onUpdate(null, true);
//        }
//    }
//}
