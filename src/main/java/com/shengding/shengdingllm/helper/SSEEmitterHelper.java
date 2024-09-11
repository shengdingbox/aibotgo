package com.shengding.shengdingllm.helper;


import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.cosntant.AdiConstant;
import com.shengding.shengdingllm.cosntant.RedisKeyConstant;
import com.shengding.shengdingllm.exception.R;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.interfaces.TriConsumer;
import com.shengding.shengdingllm.vo.SseAskParams;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.text.MessageFormat;

@Slf4j
@Service
public class SSEEmitterHelper {

    @Resource
    private RateLimitHelper rateLimitHelper;

    @Autowired
    private LLMContext llmContext;

    /**
     * 处理普通提问
     *
     * @param sseAskParams 提问参数，包含问题内容及模型选择等信息
     * @param consumer     消费型接口，用于处理提问后的回复，包含回复内容、提示元数据和答案元数据
     */
    public String commonProcess(SseAskParams sseAskParams, TriConsumer<String, String> consumer) {
        // 注册SSE事件回调，用于跟踪提问状态
        String askingKey = registerSseEventCallBack(sseAskParams);

        // 根据提问的模型名称选择对应的LLM服务，并进行普通聊天提问
        AbstractLLMService abstractLLMService = llmContext.getLLMService(sseAskParams.getModelName(),sseAskParams.getApiKey());
        if (sseAskParams.getStream()) {
            abstractLLMService.commonChat(sseAskParams, (response, chatId) -> {
                try {
                    // 处理LLM服务返回的提问结果
                    consumer.accept((String) response, chatId);
                } catch (Exception e) {
                    // 记录处理过程中的异常
                    log.error("commonProcess error", e);
                } finally {
                    // 回答结束后，删除提问状态跟踪键
                    RateLimitHelper.expireCacheMap.remove(askingKey);
                }
            });
        } else {
            R<String> chat = abstractLLMService.chat(sseAskParams);
            try {
                // 处理LLM服务返回的提问结果
                String data = chat.getData();
                JSONObject jsonObject = JSONObject.parseObject(data);
                String finalResponse = jsonObject.getString("response");
                String string = jsonObject.getString("chatId");
                consumer.accept(finalResponse, string);
                return string;
            } catch (Exception e) {
                // 记录处理过程中的异常
                log.error("commonProcess error", e);
            } finally {
                // 回答结束后，删除提问状态跟踪键
                RateLimitHelper.expireCacheMap.remove(askingKey);
            }
        }
        return null;
    }


    /**
     * 注册SSEEmiiter的回调
     *
     * @param sseAskParams
     * @return
     */
    private String registerSseEventCallBack(SseAskParams sseAskParams) {
        String askingKey = MessageFormat.format(RedisKeyConstant.USER_ASKING, sseAskParams.getApiKey());
        SseEmitter sseEmitter = sseAskParams.getSseEmitter();
        sseEmitter.onCompletion(() -> log.info("response complete,uid:{}", sseAskParams.getApiKey()));
        sseEmitter.onTimeout(() -> log.warn("sseEmitter timeout,uid:{},on timeout:{}", sseAskParams.getApiKey(), sseEmitter.getTimeout()));
        sseEmitter.onError(
                throwable -> {
                    try {
                        log.error("sseEmitter error,uid:{},on error:{}", sseAskParams.getApiKey(), throwable);
                        sseEmitter.send(SseEmitter.event().name(AdiConstant.SSEEventName.ERROR).data(throwable.getMessage()));
                    } catch (IOException e) {
                        log.error("error", e);
                    } finally {
                        RateLimitHelper.expireCacheMap.remove(askingKey);
                    }
                }
        );
        return askingKey;
    }

    /**
     * 向客户端发送错误消息并完成SSE连接
     * <p>
     * 本函数的作用是处理错误事件，通过SSE（Server-Sent Events）向特定客户端发送错误信息，
     * 然后完成（结束）SSE连接，并从本地记录中删除该SSE请求
     *
     * @param authorization 用户ID，用于识别SSE请求的客户端
     * @param sseEmitter    SSE发射器对象，用于发送事件给客户端
     * @param errorMsg      错误消息字符串，发送给客户端的信息内容
     * @throws RuntimeException 如果SSE发送失败，则抛出运行时异常
     */
    public void sendErrorAndComplete(String authorization, SseEmitter sseEmitter, String errorMsg) {
        try {
            // 尝试向客户端发送一个名为'ERROR'的事件，事件数据为错误消息
            sseEmitter.send(SseEmitter.event().name(AdiConstant.SSEEventName.ERROR).data(errorMsg));
        } catch (IOException e) {
            // 如果发送过程中发生IO异常，则转换为运行时异常并抛出，保证上层调用者能感知到错误
            throw new RuntimeException(e);
        }
        // 完成并关闭SSE连接，防止资源泄漏
        sseEmitter.complete();
        // 从本地存储中删除该用户的SSE请求，保持数据的时效性
        delSseRequesting(authorization);
    }

    /**
     * 删除用户的点播请求
     * <p>
     * 当用户取消点播请求或完成点播时，此方法用于从Redis中删除该用户的点播请求记录
     * 使用MessageFormat格式化方法名和userId，构造出Redis中记录用户点播请求的键名
     * 通过stringRedisTemplate删除Redis中对应键名的记录，从而清除该用户的点播请求信息
     *
     * @param userId 用户ID，用于定位Redis中存储的该用户点播请求的键
     */
    private void delSseRequesting(String userId) {
        String askingKey = MessageFormat.format(RedisKeyConstant.USER_ASKING, userId);
        rateLimitHelper.delete(askingKey);
    }
}
