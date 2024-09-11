package com.shengding.shengdingllm.helper;


import com.shengding.shengdingllm.exception.BaseException;
import com.shengding.shengdingllm.exception.ErrorCode;
import com.shengding.shengdingllm.interfaces.AbstractLLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * llmService上下文类（策略模式）
 */
@Slf4j
@Service
public class LLMContext {

    @Autowired
    private ApplicationContext applicationContext;


    public AbstractLLMService getLLMService(String modelName, String accessToken) {
        Map<String, AbstractLLMService> beansOfType = applicationContext.getBeansOfType(AbstractLLMService.class);
        Map<String, AbstractLLMService> collect = beansOfType.values().stream().collect(Collectors.toMap(AbstractLLMService::getMODEL_NAME, service -> service, (oldValue, newValue) -> oldValue));
        AbstractLLMService service = collect.get(modelName);
        if (null == service) {
            log.warn("︿︿︿ Can not find {}, use the default model GPT_3_5_TURBO ︿︿︿", modelName);
            throw new BaseException(ErrorCode.B_LLM_SERVICE_DISABLED, modelName);
        }
        service.setAccess_token(accessToken);
        service.setMODEL_NAME(modelName);
        return service;
    }
}
