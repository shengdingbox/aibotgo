package com.shengding.shengdingllm.interfaces;

import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.exception.BaseException;
import com.shengding.shengdingllm.exception.R;
import com.shengding.shengdingllm.utils.ResponseManager;
import com.shengding.shengdingllm.vo.*;
import io.micrometer.common.util.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import static com.shengding.shengdingllm.exception.ErrorCode.B_LLM_SERVICE_DISABLED;

@Slf4j
@Data
@Service
public abstract class AbstractLLMService {

    protected Proxy proxy;
    protected String BASE_URL;
    public String MODEL_NAME;
    protected String access_token;


    /**
     * 检测该service是否可用（不可用的情况通常是没有配置key）
     *
     * @return
     */
    public boolean isEnabled() {
        return true;
    }


    protected abstract void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam);

    protected abstract JSONObject createChatContext(String chatId);

    protected abstract boolean checkAvailability();

    protected String parseError(Object error) {
        if (error instanceof BaseException baseException) {
            return baseException.getMessage();
        }
        return Strings.EMPTY;
    }


    protected Map<String, Object> createResponse(String chatId, String content, boolean done) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("done", done);
        response.put("chatId", chatId);
        return response;
    }

    protected Map<String, Object> createErrorResponse(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "An error occurred: " + e.getMessage());
        return errorResponse;
    }


    public R<String> chat(SseAskParams sseAskParams) {
        ResponseManager manager = new ResponseManager();
        sendPrompt(sseAskParams.getAssistantChatParams(), manager::handleUpdate, manager);
        String finalResponse = null;
        try {
            JSONObject jsonObject = new JSONObject();
            finalResponse = manager.getResponse();
            jsonObject.put("response", finalResponse);
            jsonObject.put("chatId", manager.getMessageId());
            return R.succeed(jsonObject.toJSONString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 普通聊天，直接将用户提问及历史消息发送给AI
     * （由于RetrievalAugmentor强制要求提供ContentRetriever,并且prompt发给Ai前一定要过一遍本地EmbeddingStore，影响速度，故先暂时不使用Langchain4j提供的查询压缩）
     *
     * @param params
     * @param consumer
     */
    public void commonChat(SseAskParams params, TriConsumer<String, String> consumer) {
        if (!isEnabled()) {
            log.error("llm service is disabled");
            throw new BaseException(B_LLM_SERVICE_DISABLED);
        }
        AssistantChatParams assistantChatParams = params.getAssistantChatParams();
        log.info("sseChat,messageId:{}", assistantChatParams.getMessageId());
        JSONObject json = new JSONObject();
        sendPrompt(assistantChatParams, (callbackParam, response) -> {
            Object o = response.get("done");
            if (Boolean.TRUE.equals(o)) {
                String content = String.valueOf(response.get("content"));
                String chatId = String.valueOf(response.get("chatId"));
                log.info("返回数据结束了:{}", response);
                String questionUuid = StringUtils.isNotBlank(params.getRegenerateQuestionUuid()) ? params.getRegenerateQuestionUuid() : UUID.randomUUID().toString().replace("-", "");
                String userMessage = assistantChatParams.getUserMessage().getContent();
                String stopStreamToString = ChatSseResponse.stopStreamToString(chatId, MODEL_NAME, userMessage, content);
                log.info("meta:" + stopStreamToString);
                try {
                    params.getSseEmitter().send(stopStreamToString);
                    params.getSseEmitter().send(" " + AdiConstant.SSEEventName.DONE);
                } catch (IOException e) {
                    log.error("stream onComplete error", e);
                    throw new RuntimeException(e);
                }
                // close eventSourceEmitter after tokens was calculated
                params.getSseEmitter().complete();
                consumer.accept(content, chatId);
            } else {
                Object content = response.get("content");
                if (log.isDebugEnabled()) {
                    log.info("get content:{}", content);
                }
                String chatId = String.valueOf(response.get("chatId"));
                String string = ChatSseResponse.streamToString(chatId, MODEL_NAME, content.toString());
                //加空格配合前端的fetchEventSource进行解析，见https://github.com/Azure/fetch-event-source/blob/45ac3cfffd30b05b79fbf95c21e67d4ef59aa56a/src/parse.ts#L129-L133
                try {
                    params.getSseEmitter().send(string);
                } catch (IOException e) {
                    log.error("stream onNext error", e);
                }
            }
            System.out.println("Response: " + response);
        }, json);
        System.out.println(json);
    }

    public  void sendMessage(String phone){

    }
}
