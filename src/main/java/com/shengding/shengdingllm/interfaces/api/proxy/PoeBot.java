//package com.shengding.shengdingllm.interfaces.impl;
//
//import okhttp3.*;
//import okhttp3.sse.EventSource;
//import okhttp3.sse.EventSourceListener;
//import okhttp3.sse.EventSources;
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import java.io.IOException;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//public class PoeBot {
//    private static final String BRAND_ID = "poe";
//    private static final String CLASS_NAME = "PoeBot";
//    private static final String LOGO_FILENAME = "default-logo.svg";
//    private static final String LOGIN_URL = "https://poe.com/";
//
//    private String buildId;
//    private int chatId;
//    private JSONObject settings;
//    private long lastMessageId = 0;
//
//    private final OkHttpClient client = new OkHttpClient();
//
//    public PoeBot() {}
//
//    private String generateTagId(String payload, String formkey) throws NoSuchAlgorithmException {
//        MessageDigest md = MessageDigest.getInstance("MD5");
//        String input = payload + formkey + "4LxgHM6KpFqokX0Ox";
//        byte[] hash = md.digest(input.getBytes());
//        StringBuilder sb = new StringBuilder();
//        for (byte b : hash) {
//            sb.append(String.format("%02x", b));
//        }
//        return sb.toString();
//    }
//
//    public boolean checkAvailability() {
//        boolean available = false;
//        Map<String, String> modelHandles = new HashMap<>();
//        modelHandles.put("a2", "Claude-instant");
//        modelHandles.put("a2_100k", "Claude-instant-100k");
//        modelHandles.put("a2_2", "Claude-2-100k");
//        modelHandles.put("capybara", "Assistant");
//        modelHandles.put("chinchilla", "ChatGPT");
//        modelHandles.put("beaver", "GPT-4");
//        modelHandles.put("vizcacha", "GPT-4-32k");
//        modelHandles.put("code_llama_34b_instruct", "Code-Llama-34b");
//        modelHandles.put("acouchy", "Google-PaLM-2");
//        modelHandles.put("llama_2_70b_chat", "Llama-2-70b");
//
//        String url = LOGIN_URL + modelHandles.getOrDefault(CLASS_NAME, "Assistant");
//
//        Request request = new Request.Builder().url(url).build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (response.body() != null) {
//                String responseBody = response.body().string();
//                buildId = responseBody.split("\"buildId\":\"")[1].split("\",")[0];
//                chatId = Integer.parseInt(responseBody.split("\"chatId\":")[1].split(",")[0]);
//                available = true;
//            }
//        } catch (IOException e) {
//            log.error("Error checking Poe login status: " + e.getMessage());
//        }
//
//        return available;
//    }
//
//    public void sendPrompt(String prompt, UpdateResponseCallback onUpdateResponse, Object callbackParam) {
//        try {
//            // Fetch settings if not already available
//            if (settings == null) {
//                Request request = new Request.Builder().url("https://poe.com/api/settings").build();
//                try (Response response = client.newCall(request).execute()) {
//                    if (response.body() != null) {
//                        settings = new JSONObject(response.body().string());
//                    }
//                }
//            }
//
//            String subDomain = "tch" + (int) (Math.random() * 1000000);
//            JSONObject tchannel = settings.getJSONObject("tchannelData");
//            String url = String.format("https://%s.tch.%s/up/%s/updates?min_seq=%d&channel=%s&hash=%s",
//                    subDomain, tchannel.getString("baseHost"), tchannel.getString("boxName"),
//                    tchannel.getLong("minSeq"), tchannel.getString("channel"), tchannel.getString("channelHash"));
//
//            Request sseRequest = new Request.Builder().url(url).build();
//
//            EventSource.Factory factory = EventSources.createFactory(client);
//            factory.newEventSource(sseRequest, new EventSourceListener() {
//                @Override
//                public void onEvent(EventSource eventSource, String id, String type, String data) {
//                    try {
//                        JSONObject message = new JSONObject(data);
//                        if (message.has("error")) {
//                            onUpdateResponse.onError(new Exception(message.getString("error")));
//                            eventSource.cancel();
//                            return;
//                        }
//
//                        settings.getJSONObject("tchannelData").put("minSeq", message.getLong("min_seq"));
//                        JSONArray messages = message.getJSONArray("messages");
//                        for (int i = 0; i < messages.length(); i++) {
//                            JSONObject m = new JSONObject(messages.getString(i));
//                            if ("subscriptionUpdate".equals(m.getString("message_type"))) {
//                                JSONObject messageAdded = m.getJSONObject("payload").getJSONObject("data").getJSONObject("messageAdded");
//                                if (messageAdded != null && messageAdded.getLong("messageId") > lastMessageId
//                                        && CLASS_NAME.equals(messageAdded.getString("author"))) {
//                                    boolean done = "complete".equals(messageAdded.getString("state"));
//                                    onUpdateResponse.onUpdate(callbackParam, messageAdded.getString("text"), done);
//                                    if (done) {
//                                        lastMessageId = messageAdded.getLong("messageId");
//                                        eventSource.cancel();
//                                    }
//                                }
//                            }
//                        }
//                    } catch (Exception e) {
//                        onUpdateResponse.onError(e);
//                        eventSource.cancel();
//                    }
//                }
//
//                @Override
//                public void onFailure(EventSource eventSource, Throwable t, Response response) {
//                    onUpdateResponse.onError(t);
//                    eventSource.cancel();
//                }
//            });
//
//            String payload = new JSONObject()
//                    .put("bot", CLASS_NAME)
//                    .put("chatId", chatId)
//                    .put("query", prompt)
//                    .put("source", JSONObject.NULL)
//                    .put("withChatBreak", false)
//                    .toString();
//
//            String tagId = generateTagId(payload, settings.getString("formkey"));
//
//            Request postRequest = new Request.Builder()
//                    .url("https://poe.com/api/gql_POST")
//                    .post(RequestBody.create(payload, MediaType.parse("application/json")))
//                    .addHeader("Content-Type", "application/json")
//                    .addHeader("poe-formkey", settings.getString("formkey"))
//                    .addHeader("poe-tchannel", tchannel.getString("channel"))
//                    .addHeader("poe-tag-id", tagId)
//                    .build();
//
//            client.newCall(postRequest).enqueue(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    onUpdateResponse.onError(e);
//                }
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    if (!response.isSuccessful() || response.body() == null) {
//                        onUpdateResponse.onError(new IOException("Unexpected code " + response));
//                    }
//                }
//            });
//
//        } catch (Exception e) {
//            onUpdateResponse.onError(e);
//        }
//    }
//
//    public interface UpdateResponseCallback {
//        void onUpdate(Object callbackParam, String content, boolean done);
//
//        void onError(Throwable t);
//    }
//}
