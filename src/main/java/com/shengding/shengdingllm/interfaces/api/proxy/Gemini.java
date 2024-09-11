package com.shengding.shengdingllm.interfaces.api.proxy;//package org.example.bot;
//
//import okhttp3.*;
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
//public class Gemini extends Bot {
//    private static final String BRAND_ID = "bard";
//    private static final String CLASS_NAME = "BardBot";
//    private static final String MODEL = "gemini-pro"; // gemini-pro or gemini-ultra
//    private static final String LOGO_FILENAME = "gemini-chat-logo.svg";
//    private static final String LOGIN_URL = "https://gemini.google.com/";
//    private static final String USER_AGENT =
//            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) chatall/1.29.40 Chrome/114.0.5735.134 Safari/537.36";
//
//    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
//            .readTimeout(0, TimeUnit.MILLISECONDS)
//            .build();
//
//    public Gemini() {
//        super();
//    }
//
//    private boolean checkAvailability() throws IOException {
//        JSONObject context = getChatContext();
//        return context != null && context.has("requestParams") && context.getJSONObject("requestParams").has("atValue");
//    }
//
//    public void sendPrompt(String prompt, UpdateResponseCallback onUpdateResponse, Object callbackParam) throws IOException {
//        JSONObject context = getChatContext();
//        String atValue = context.getJSONObject("requestParams").getString("atValue");
//        String blValue = context.getJSONObject("requestParams").getString("blValue");
//        JSONArray contextIds = context.getJSONArray("contextIds");
//
//        RequestBody body = new FormBody.Builder()
//                .add("at", atValue)
//                .add("f.req", generateReq(MODEL, prompt, contextIds))
//                .build();
//
//        Request request = new Request.Builder()
//                .url("https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate")
//                .post(body)
//                .addHeader("User-Agent", USER_AGENT)
//                .addHeader("Content-Type", "application/x-www-form-urlencoded")
//                .build();
//
//        CLIENT.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                onUpdateResponse.onError(e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                String resp = response.body().string();
//                try {
//                    ParseResult result = parseResponse(resp);
//                    context.put("contextIds", new JSONArray(result.ids));
//                    setChatContext(context);
//                    onUpdateResponse.onUpdate(callbackParam, result.text, true);
//                } catch (Exception e) {
//                    onUpdateResponse.onError(e);
//                }
//            }
//        });
//    }
//
//    private JSONObject createChatContext() throws IOException {
//        Request request = new Request.Builder()
//                .url("https://gemini.google.com/app")
//                .addHeader("User-Agent", USER_AGENT)
//                .build();
//
//        try (Response response = CLIENT.newCall(request).execute()) {
//            if (response.isSuccessful()) {
//                String body = response.body().string();
//                String atValue = extractValue(body, "\"SNlM0e\":\"([^\"]+)\"");
//                String blValue = extractValue(body, "\"cfb2h\":\"([^\"]+)\"");
//
//                if (atValue == null || blValue == null) {
//                    throw new IOException("Failed to fetch Bard at/bl values");
//                }
//
//                JSONObject context = new JSONObject();
//                context.put("requestParams", new JSONObject().put("atValue", atValue).put("blValue", blValue));
//                context.put("contextIds", new JSONArray().put("").put("").put(""));
//                return context;
//            } else {
//                throw new IOException("Failed to create chat context");
//            }
//        }
//    }
//
//    private String extractValue(String body, String regex) {
//        return body.matches(regex) ? body.replaceAll(regex, "$1") : null;
//    }
//
//    private String generateReq(String model, String prompt, JSONArray contextIds) {
//        int modelNumber = "gemini-ultra".equals(model) ? 2 : 1;
//
//        JSONArray innerJSON = new JSONArray()
//                .put(new JSONArray().put(prompt).put(0).put(JSONObject.NULL).put(JSONObject.NULL).put(JSONObject.NULL).put(JSONObject.NULL).put(0))
//                .put(new JSONArray().put("en"))
//                .put(contextIds)
//                .put("")
//                .put("")
//                .put(JSONObject.NULL)
//                .put(new JSONArray().put(1))
//                .put(0)
//                .put(JSONObject.NULL)
//                .put(JSONObject.NULL)
//                .put(1)
//                .put(0)
//                .put(JSONObject.NULL)
//                .put(JSONObject.NULL)
//                .put(JSONObject.NULL)
//                .put(JSONObject.NULL)
//                .put(JSONObject.NULL)
//                .put(JSONObject.NULL)
//                .put(modelNumber);
//
//        return new JSONArray().put(JSONObject.NULL).put(innerJSON.toString()).toString();
//    }
//
//    private ParseResult parseResponse(String resp) throws Exception {
//        String[] lines = resp.split("\n");
//        if (lines.length < 4) {
//            throw new Exception("Invalid response format");
//        }
//
//        JSONObject data = new JSONObject(lines[3]);
//        data = new JSONObject(data.getJSONArray("0").getString(2));
//
//        if (data == null) {
//            throw new Exception("Failed to parse Bard response");
//        }
//
//        JSONArray idsArray = data.getJSONArray("1");
//        idsArray.put(data.getJSONArray("4").getJSONArray(0).getString(0));
//
//        String text = data.getJSONArray("4").getJSONArray(0).getJSONArray(1).getString(0);
//        JSONArray images = data.getJSONArray("4").getJSONArray(0).optJSONArray(4);
//
//        if (images != null) {
//            for (int i = 0; i < images.length(); i++) {
//                JSONArray image = images.getJSONArray(i);
//                String url = image.getJSONArray(0).getJSONArray(0).getString(0);
//                String alt = image.getJSONArray(0).getString(4);
//                String link = image.getJSONArray(1).getJSONArray(0).getString(0);
//                String placeholder = image.getString(2);
//                text = text.replace(placeholder, String.format("[![%s](%s)](%s \"%s\")", alt, url, link, link));
//            }
//        }
//
//        ParseResult result = new ParseResult();
//        result.text = text;
//        result.ids = idsArray.toList().toArray(new String[0]);
//        return result;
//    }
//
//    private static class ParseResult {
//        String text;
//        String[] ids;
//    }
//
//    public interface UpdateResponseCallback {
//        void onUpdate(Object callbackParam, String content, boolean done);
//
//        void onError(Throwable t);
//    }
//}
