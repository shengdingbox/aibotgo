package com.shengding.shengdingllm.interfaces.api.proxy;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import okio.ByteString;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CharacterAIBot {
    private static final String BRAND_ID = "characterAI";
    private static final String CLASS_NAME = "CharacterAIBot";
    private static final String LOGO_FILENAME = "character-ai-logo.svg";
    private static final boolean IS_DARK_LOGO = true;
    private static final String LOGIN_URL = "https://character.ai/";

    private String characterId;
    private String username;
    private String userId;
    private String chatId;
    private boolean isFirstMessage = true;

    public CharacterAIBot(String characterId, String username, String userId) {
        this.characterId = characterId;
        this.username = username;
        this.userId = userId;
        this.chatId = UUID.randomUUID().toString();
    }

    public boolean checkAvailability() {
        // Implement availability check logic if needed
        return characterId != null && username != null && userId != null;
    }

    public void sendPrompt(String prompt, UpdateResponseCallback onUpdateResponse, Object callbackParam) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("wss://neo.character.ai/ws/")
                .build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                if (isFirstMessage) {
                    isFirstMessage = false;
                    webSocket.send(createChatMessage());
                }
                webSocket.send(createTurnMessage(prompt));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleWebSocketMessage(text, onUpdateResponse, callbackParam, webSocket);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                handleWebSocketMessage(bytes.utf8(), onUpdateResponse, callbackParam, webSocket);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                onUpdateResponse.onError(t);
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                onUpdateResponse.onUpdate(callbackParam, "", true);
            }
        };

        client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }

    private void handleWebSocketMessage(String text, UpdateResponseCallback onUpdateResponse, Object callbackParam, WebSocket webSocket) {
        try {
            JSONObject data = JSONObject.parseObject(text);
            if (data.containsKey("command") && data.getString("command").equals("neo_error")) {
                throw new Exception(data.toString());
            }

            if (data.containsKey("turn")) {
                JSONObject turn = data.getJSONObject("turn");
                if (turn.getJSONObject("author").getString("author_id").equals(characterId)) {
                    JSONArray jsonArray = turn.getJSONArray("candidates");
                    String content = jsonArray.stream()
                            .map(candidate -> ((Map) candidate).get("raw_content").toString())
                            .collect(Collectors.joining("\n---\n"));

                    onUpdateResponse.onUpdate(callbackParam, content, false);

                    JSONArray candidates = turn.getJSONArray("candidates");
                    boolean isFinal = candidates.stream()
                            .allMatch(candidate -> ((JSONObject) candidate).getBoolean("is_final"));

                    if (isFinal) {
                        onUpdateResponse.onUpdate(callbackParam, "", true);
                        webSocket.close(1000, null);
                    }
                }
            }
        } catch (Exception e) {
            onUpdateResponse.onError(e);
            webSocket.close(1000, null);
        }
    }

    private String createChatMessage() {
        JSONObject payload = new JSONObject();
        payload.put("command", "create_chat");
        payload.put("request_id", generateRequestId());
        payload.put("payload", new JSONObject());
        payload.put("chat", new JSONObject());
        payload.put("chat_id", chatId);
        payload.put("creator_id", userId);
        payload.put("visibility", "VISIBILITY_PRIVATE");
        payload.put("character_id", characterId);
        payload.put("type", "TYPE_ONE_ON_ONE");
        payload.put("with_greeting", false);
        payload.put("origin_id", "web-next");
        return payload.toString();
    }

    private String createTurnMessage(String prompt) {
        String turnId = UUID.randomUUID().toString();
        JSONObject payload = new JSONObject();
        payload.put("command", "create_and_generate_turn");
        payload.put("request_id", generateRequestId());
        payload.put("payload", new JSONObject());
        payload.put("num_candidates", 1);
        payload.put("tts_enabled", false);
        payload.put("selected_language", "");
        payload.put("character_id", characterId);
        payload.put("user_name", username);
        payload.put("turn", new JSONObject());
        payload.put("turn_key", new JSONObject());
        payload.put("turn_id", turnId);
        payload.put("chat_id", chatId);
        payload.put("author", new JSONObject());
        payload.put("author_id", userId);
        payload.put("is_human", true);
        payload.put("name", username);
        payload.put("candidates", new JSONArray());
//        payload.put(new JSONObject());
        payload.put("candidate_id", turnId);
        payload.put("raw_content", prompt);
        payload.put("primary_candidate_id", turnId);
        payload.put("origin_id", "web-next");
        return payload.toString();
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().replaceFirst("-[0-9a-f]{4}-[0-9a-f]{4}-", "-vhj9cXafWcF8-");
    }

    public interface UpdateResponseCallback {
        void onUpdate(Object callbackParam, String content, boolean done);

        void onError(Throwable t);
    }

    public static void main(String[] args) {
    }
}
