package com.shengding.shengdingllm;

import okhttp3.*;

import java.io.IOException;

public class deep {
    public static void main(String[] args) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n    \"message\": \"123\",\n    \"stream\": true,\n    \"model_preference\": null,\n    \"model_class\": \"deepseek_chat\",\n    \"temperature\": 0\n}");
        Request request = new Request.Builder()
                .url("https://chat.deepseek.com/api/v0/chat/completions")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer 4159c39884b44080a567d2fb5af6bd80")
                .addHeader("Cookie", "route=792d6435ac864d2e3473d227f34d5549|6f8463e6e0fc28568ed5f94061f80538; HWWAFSESID=df70c8202a161b602e; HWWAFSESTIME=1728445150464")
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
    }
}
