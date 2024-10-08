package com.shengding.shengdingllm.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.api.request.ChatRequest;
import com.shengding.shengdingllm.api.request.Message;
import com.shengding.shengdingllm.cosntant.ChatMessageRoleEnum;
import com.shengding.shengdingllm.helper.LLMContext;
import com.shengding.shengdingllm.helper.SSEEmitterHelper;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import com.shengding.shengdingllm.vo.ChatSseResponse;
import com.shengding.shengdingllm.vo.LLMBuilderProperties;
import com.shengding.shengdingllm.vo.SseAskParams;
import com.zhouzifei.cache.FileCacheEngine;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {

    @Resource
    private SSEEmitterHelper sseEmitterHelper;

    @Autowired
    private LLMContext llmContext;


    /**
     * 异步检查并推送消息到客户端
     *
     * @param sseEmitter    用于发送服务器推送事件的发射器
     * @param authorization 当前用户信息
     * @param chatRequest   询问请求对象，包含对话和问题的详细信息
     */
    public ChatSseResponse completions(SseEmitter sseEmitter, String authorization, ChatRequest chatRequest) {
        // 记录进入方法的用户ID
        log.info("asyncCheckAndPushToClient,userId:{}", authorization);
        // 检查业务规则，如果不满足则返回
//        if (!checkConversation(sseEmitter, authorization, chatRequest)) {
//            return sseEmitter;
//        }

        // 初始化问题参数对象
        SseAskParams sseAskParams = new SseAskParams();
        String modelName = chatRequest.getModel().toUpperCase();
        sseAskParams.setModelName(modelName);
        sseAskParams.setSseEmitter(sseEmitter);
        //sseAskParams.setRegenerateQuestionUuid(askReq.getRegenerateQuestionUuid());
        sseAskParams.setApiKey(authorization);
        sseAskParams.setStream(chatRequest.getStream());
        sseAskParams.setUseSearch(chatRequest.getUse_search());

        // 构建助手参数
        AssistantChatParams.AssistantChatParamsBuilder assistantBuilder = AssistantChatParams.builder();
        // 如果对话有系统消息，则设置到助手参数中
        if (null != chatRequest.getSystemMessage()) {
            assistantBuilder.systemMessage(chatRequest.getSystemMessage());
        }
        // 获取并设置用户问题
        Message message = chatRequest.getUserMessage();
        assistantBuilder.userMessage(message);
        //如果对话启用了上下文理解，则设置消息ID到助手参数中
        String questionUuid = getQuestionUuid(chatRequest);
        if (StringUtils.isNotBlank(questionUuid)) {
            assistantBuilder.messageId(questionUuid);
        }
        final List<Message>[] messages = new List[]{chatRequest.getMessages()};
        assistantBuilder.messages(messages[0]);
        sseAskParams.setAssistantChatParams(assistantBuilder.build());
        // 设置LLM（Language Model）属性
        sseAskParams.setLlmBuilderProperties(
                LLMBuilderProperties.builder()
                        .temperature(0.7)
                        .build()
        );
        if (sseAskParams.getStream()) {
            sseEmitterHelper.commonProcess(sseAskParams, (response, chatId) -> {
                resetMessage(messages[0], chatId, chatRequest);
            });
        } else {
            JSONObject jsonObject = sseEmitterHelper.commonProcess(sseAskParams);
            String finalResponse = jsonObject.getString("response");
            String chatId = jsonObject.getString("chatId");
            String userMessage = JSONObject.toJSONString(messages[0]);
            String stopStreamToString = ChatSseResponse.chatString(chatId,modelName ,userMessage ,finalResponse);
            resetMessage(messages[0], chatId, chatRequest);
            ChatSseResponse chatSseResponse = JSONObject.parseObject(stopStreamToString, ChatSseResponse.class);
            return chatSseResponse;
        }
        // 处理SSE发射器并保存AI响应后的数据
        return null;
    }

    public void resetMessage(List<Message> messages, String chatId, ChatRequest chatRequest){
        FileCacheEngine fileCacheEngine = new FileCacheEngine();
        messages = messages.stream().filter(msg -> ChatMessageRoleEnum.USER.getValue().equals(msg.getRole())).collect(Collectors.toList());
        String jsonString = JSONObject.toJSONString(messages);
        String md5Hex = DigestUtil.md5Hex(jsonString.getBytes(StandardCharsets.UTF_8));
        fileCacheEngine.add(chatRequest.getModel(), md5Hex, chatId);
        // 移除最后一个元素
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
        }
        String jsonStringAfter = JSONObject.toJSONString(messages);
        String md5HexAfter = DigestUtil.md5Hex(jsonStringAfter.getBytes(StandardCharsets.UTF_8));
        fileCacheEngine.remove(chatRequest.getModel(), md5HexAfter);
    }
    private String getQuestionUuid(ChatRequest chatRequest) {
        List<Message> messages = chatRequest.getMessages().stream().filter(message -> ChatMessageRoleEnum.USER.getValue().equals(message.getRole())).collect(Collectors.toList());
        // 移除最后一个元素
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
        }
        FileCacheEngine fileCacheEngine = new FileCacheEngine();
        String jsonString = JSONObject.toJSONString(messages);
        String md5Hex = DigestUtil.md5Hex(jsonString.getBytes(StandardCharsets.UTF_8));
        String chatId = fileCacheEngine.get(chatRequest.getModel(), md5Hex, String.class);
        return chatId;
    }

    /**
     * 检查用户对话的合法性
     *
     * @param sseEmitter    用于推送事件的服务器发送事件发射器
     * @param authorization 当前用户信息
     * @param chatRequest   询问请求对象，包含对话UUID等信息
     * @return 如果对话合法，则返回true；否则执行相应操作并返回false
     */
    private boolean checkConversation(SseEmitter sseEmitter, String authorization, ChatRequest chatRequest) {
//        try {
//            // 检查对话是否已被删除
//            Conversation delConv = conversationService.lambdaQuery()
//                    .eq(Conversation::getUuid, askReq.getConversationUuid())
//                    .eq(Conversation::getIsDeleted, true)
//                    .one();
//            if (null != delConv) {
//                // 对话已删除，发送错误信息并完成对话
//                sseEmitterHelper.sendErrorAndComplete(user.getId(), sseEmitter, "该对话已经删除");
//                return false;
//            }
//            // 检查用户对话数量是否达到上限
//            Long convsCount = conversationService.lambdaQuery()
//                    .eq(Conversation::getUserId, user.getId())
//                    .eq(Conversation::getIsDeleted, false)
//                    .count();
//            long convsMax = Integer.parseInt(systemConfig.getConversationMaxNum());
//            if (convsCount >= convsMax) {
//                // 对话数量达到上限，发送错误信息并完成对话
//                sseEmitterHelper.sendErrorAndComplete(user.getId(), sseEmitter, "对话数量已经达到上限，当前对话上限为：" + convsMax);
//                return false;
//            }
//
//            // 检查当前用户是否还有文本配额
//            ErrorEnum errorMsg = quotaHelper.checkTextQuota(user);
//            if (null != errorMsg) {
//                // 用户文本配额不足，发送错误信息并完成对话
//                sseEmitterHelper.sendErrorAndComplete(user.getId(), sseEmitter, errorMsg.getInfo());
//                return false;
//            }
//        } catch (Exception e) {
//            // 异常处理：记录日志并发送错误事件
//            log.error("error", e);
//            sseEmitter.completeWithError(e);
//            return false;
//        }
        // 对话合法，返回true
        return true;
    }

    public void sendMessage(String modelName, String phone) {
        AbstractLLMService abstractLLMService = llmContext.getLLMService(modelName);
        abstractLLMService.sendMessage(phone);
    }
}
