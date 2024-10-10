package com.shengding.shengdingllm.interfaces.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.api.request.Message;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.interfaces.EventSourceStreamListener;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import io.micrometer.common.lang.Nullable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Service
@Slf4j
public class KimiLLMService extends AbstractLLMService {

    // 构造函数，初始化模型名称和基础URL
    public KimiLLMService() {
        MODEL_NAME = AdiConstant.ModelPlatform.KIMI;
        BASE_URL = "https://kimi.moonshot.cn";
    }

    // 配置OkHttpClient，设置超时时间和主机名验证器
    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(50, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .hostnameVerifier((hostname, session) -> true)
            .build();

    // 获取认证头信息
    private Map<String, String> getAuthHeader() {
        Map<String, String> pairs = new HashMap<>();
        pairs.put("accept", "application/json, text/plain, */*");
        pairs.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,zh-HK;q=0.5,zh-TW;q=0.4");
        pairs.put("authorization", "Bearer " + access_token);
        pairs.put("cache-control", "no-cache");
        pairs.put("cookie", "Hm_lvt_358cae4815e85d48f7e8ab7f3680a74b=1725434543; Hm_lpvt_358cae4815e85d48f7e8ab7f3680a74b=17254 34544; HMACCOUNT=6DA7B554009CDBE3; _ga=GA1.1.1942321256.1725434544; _gcl_au=1.1.1730639990.1725434544; _ga_YXD8W70SZP=GS1.1.1725441294.2.0.1725441294.0.0.0");
        pairs.put("pragma", "no-cache");
        pairs.put("priority", "u=1, i");
        pairs.put("referer", "https://kimi.moonshot.cn/chat/crc2a3atnn0k573sij8g");
        pairs.put("sec-ch-ua", "Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128");
        pairs.put("sec-ch-ua-mobile", "?0");
        pairs.put("sec-ch-ua-platform", "macOS");
        pairs.put("sec-fetch-dest", "empty");
        pairs.put("sec-fetch-mode", "cors");
        pairs.put("upgrade-insecure-requests", "1");
        pairs.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
        // 获取访问令牌
        return pairs;
    }

    // 检查服务可用性
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

    // 发送消息（未实现）
    @Override
    public void sendMessage(String phone) {
        // 未实现
    }

    // 刷新访问令牌
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

    // 发送提示信息
    @Override
    public void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {
        List<Message> messages = assistantChatParams.getMessages();
////        List<String> fileUrls = extractRefFileUrls(messages);
//        List<String> fileIds = new ArrayList<>();
//
//        // 上传文件
//        for (String fileUrl : fileUrls) {
//            try {
//                String fileId = uploadFile(fileUrl);
//                fileIds.add(fileId);
//            } catch (Exception e) {
//                log.error("Error uploading file: " + e.getMessage());
//            }
//        }

        // 创建聊天上下文和请求体
        JSONObject context = createChatContext(assistantChatParams.getMessageId());
        JSONObject jsonObject = new JSONObject();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", assistantChatParams.getUserMessage().getContent());
        JSONArray objects = new JSONArray();
        objects.add(message);
        jsonObject.put("messages", objects);
        jsonObject.put("refs", new JSONArray());
        jsonObject.put("use_search", true);
        jsonObject.put("kimiplus_id", "kimi");
        JSONObject extend = new JSONObject();
        extend.put("sidebar", true);
        jsonObject.put("extend", extend);
        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));

        final String chatId = context.getString("chatId");
        Request.Builder post = new Request.Builder()
                .url(BASE_URL + "/api/chat/" + chatId + "/completion/stream")
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

    // 从消息中提取文件URL
//    private List<String> extractRefFileUrls(List<Message> messages) {
//        List<String> urls = new ArrayList<>();
//        if (messages.isEmpty()) {
//            return urls;
//        }
//        Message lastMessage = messages.get(messages.size() - 1);
//        if (lastMessage.getArrContent() instanceof List) {
//            List<Object> content = (List<Object>) lastMessage.getContent();
//            for (Object item : content) {
//                if (item instanceof Map) {
//                    Map<String, Object> itemMap = (Map<String, Object>) item;
//                    String type = (String) itemMap.get("type");
//                    if ("file".equals(type) || "image_url".equals(type)) {
//                        Map<String, Object> urlMap = (Map<String, Object>) itemMap.get(type.equals("file") ? "file_url" : "image_url");
//                        if (urlMap != null && urlMap.containsKey("url")) {
//                            urls.add((String) urlMap.get("url"));
//                        }
//                    }
//                }
//            }
//        }
//        log.info("本次请求上传：" + urls.size() + "个文件");
//        return urls;
//    }

    // 上传文件
    private String uploadFile(String fileUrl) throws Exception {
        checkFileUrl(fileUrl);

        String filename;
        byte[] fileData;
        String mimeType;

        if (isBase64Data(fileUrl)) {
            mimeType = extractBase64DataFormat(fileUrl);
            String ext = getMimeExtension(mimeType);
            filename = UUID.randomUUID() + "." + ext;
            fileData = Base64.getDecoder().decode(removeBase64DataHeader(fileUrl));
        } else {
            filename = getFilenameFromUrl(fileUrl);
            fileData = downloadFile(fileUrl);
            mimeType = getMimeType(fileData, filename);
        }

        JSONObject preSignResult = preSignUrl(filename);
        String uploadUrl = preSignResult.getString("url");
        String objectName = preSignResult.getString("object_name");

        uploadFileToOSS(uploadUrl, fileData, mimeType);

        String fileId = getFileId(filename, objectName);
        waitForFileProcessing(fileId);

        return fileId;
    }

    // 检查文件URL是否有效
    private void checkFileUrl(String fileUrl) throws Exception {
        if (isBase64Data(fileUrl)) {
            return;
        }
        Request request = new Request.Builder()
                .url(fileUrl)
                .head()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("File URL is not valid: " + response.code() + " " + response.message());
            }
            if (response.headers().get("Content-Length") != null) {
                long fileSize = Long.parseLong(response.headers().get("Content-Length"));
                if (fileSize > 1024 * 1000000) {
                    throw new Exception("File exceeds maximum size limit");
                }
            }
        }
    }

    // 检查是否为Base64编码的数据
    private boolean isBase64Data(String data) {
        return data.startsWith("data:");
    }

    // 从Base64数据中提取MIME类型
    private String extractBase64DataFormat(String data) {
        return data.split(";")[0].split(":")[1];
    }

    // 移除Base64数据的头部
    private String removeBase64DataHeader(String data) {
        return data.split(",")[1];
    }

    // 获取MIME类型对应的文件扩展名
    private String getMimeExtension(String mimeType) {
        // Implementation to get file extension from MIME type
        return "txt"; // Placeholder
    }

    // 从URL中提取文件名
    private String getFilenameFromUrl(String url) {
        // Implementation to extract filename from URL
        return "file.txt"; // Placeholder
    }

    // 下载文件
    private byte[] downloadFile(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to download file: " + response.code() + " " + response.message());
            }

            return response.body().bytes();
        }
    }

    // 获取文件的MIME类型
    private String getMimeType(byte[] data, String filename) {
        // Implementation to get MIME type from file data and filename
        return "application/octet-stream"; // Placeholder
    }

    // 获取预签名URL
    private JSONObject preSignUrl(String filename) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("action", "file");
        jsonObject.put("name", filename);
        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));
        Request.Builder post = new Request.Builder()
                .url(BASE_URL + "/api/pre-sign-url")
                .post(body);
        getAuthHeader().forEach(post::addHeader);
        Request request = post.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to get pre-signed URL: " + response.code() + " " + response.message());
            }
            return JSONObject.parseObject(response.body().string());
        }
    }

    // 上传文件到OSS
    private void uploadFileToOSS(String uploadUrl, byte[] fileData, String mimeType) throws Exception {
        RequestBody body = RequestBody.create(fileData, MediaType.parse(mimeType));

        Request request = new Request.Builder()
                .url(uploadUrl)
                .put(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to upload file to OSS: " + response.code() + " " + response.message());
            }
        }
    }

    // 获取文件ID
    private String getFileId(String filename, String objectName) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "file");
        jsonObject.put("name", filename);
        jsonObject.put("object_name", objectName);

        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));

        Request.Builder post = new Request.Builder()
                .url(BASE_URL + "/api/file")
                .post(body);
        getAuthHeader().forEach(post::addHeader);
        Request request = post.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to get file ID: " + response.code() + " " + response.message());
            }

            JSONObject responseJson = JSONObject.parseObject(response.body().string());
            return responseJson.getString("id");
        }
    }

    // 等待文件处理完成
    private void waitForFileProcessing(String fileId) throws Exception {
        String status = "";
        long startTime = System.currentTimeMillis();

        while (!"initialized".equals(status)) {
            if (System.currentTimeMillis() - startTime > 30000) {
                throw new Exception("File processing timeout");
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ids", new JSONArray().fluentAdd(fileId));

            RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));

            Request.Builder post = new Request.Builder()
                    .url(BASE_URL + "/api/file/parse_process")
                    .post(body);
            getAuthHeader().forEach(post::addHeader);
            Request request = post.build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new Exception("Failed to check file processing status: " + response.code() + " " + response.message());
                }

                String string = response.body().string();
                JSONObject responseJson = JSONObject.parseObject(string);
                status = responseJson.getString("status");
            }

            Thread.sleep(1000);
        }
    }

    // 创建聊天上下文
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

    public static void main(String[] args) throws Exception {
//        com.shengding.shengdingllm.utils.ResponseManager manager = new ResponseManager();
//        String finalResponse = null;
//        System.setProperty("http.proxyHost", "127.0.0.1");
//        System.setProperty("http.proxyPort", Integer.toString(8888));
//        Message message = new Message();
//        message.setContent("你好");
//        AssistantChatParams assistantChatParams = AssistantChatParams.builder()
//                .userMessage(message).build();
//        new KimiLLMService().sendPrompt(assistantChatParams, manager::handleUpdate, manager);
//        try {
//            finalResponse = manager.getResponse();
//            log.info(finalResponse);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        KimiLLMService kimiLLMService = new KimiLLMService();
        kimiLLMService.setAccess_token("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ1c2VyLWNlbnRlciIsImV4cCI6MTcyNjgyMzMxOCwiaWF0IjoxNzI2ODIyNDE4LCJqdGkiOiJjcm1qZzRtYmk3czJ0Y2x1YTVnZyIsInR5cCI6ImFjY2VzcyIsImFwcF9pZCI6ImtpbWkiLCJzdWIiOiJjcjBjbWRlc2R2MTQ3ZDY1ZDVvMCIsInNwYWNlX2lkIjoiY3IwY21kZXNkdjE0N2Q2NWQ1bmciLCJhYnN0cmFjdF91c2VyX2lkIjoiY3IwY21kZXNkdjE0N2Q2NWQ1bjAifQ.knXmwkU-GDNZfE75DFrHIiVTeqGu0ahvd-Vv-90lJ1wPoczenn7vsVlarc46U6Zo8MP-HNlChyzx9g-MURoE2g");
        kimiLLMService.uploadFile("https://mj101-1317487292.cos.ap-shanghai.myqcloud.com/ai/test.pdf");
    }
}
