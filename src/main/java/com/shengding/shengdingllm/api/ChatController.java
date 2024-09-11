package com.shengding.shengdingllm.api;

import com.shengding.shengdingllm.api.request.ChatRequest;
import com.shengding.shengdingllm.exception.BaseException;
import com.shengding.shengdingllm.exception.ErrorCode;
import com.shengding.shengdingllm.service.ChatService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1")
public class ChatController {

    @Autowired
    HttpServletRequest request;

    @Autowired
    private ChatService chatService;

    @PostMapping(value = "/chat/completions")
    public Object completions(@RequestBody @Validated ChatRequest chatRequest) {
        String authorization = request.getHeader("authorization");
        if (authorization.contains("Bearer ")) {
            authorization = authorization.replace("Bearer ", "");
        }
        if (StringUtils.isEmpty(authorization)) {
            throw new BaseException(ErrorCode.A_USER_NOT_AUTH.getCode(), "未登录");

        }
        // 如果需要 SSE 流，则创建 SseEmitter 对象
        SseEmitter sseEmitter = new SseEmitter();
        if (chatRequest.getStream()) {
            chatService.completions(sseEmitter, authorization, chatRequest);
            return sseEmitter;
        } else {
            // 如果不需要 SSE 流，则返回 JSON 对象
            return chatService.completions(sseEmitter, authorization, chatRequest);
        }
    }


    @PostMapping("/session")
    public String index(String message) {
        return "{status: \"Success\", message: \"\", data: {auth: false, model: \"ChatGPTUnofficialProxyAPI\"}}";
    }

//    @PostMapping("/config")
//    public R<ConfigRequest> config() {
//        ConfigRequest configRequest = new ConfigRequest();
//        configRequest.setApiModel("gpt-3.5-turbo");
//        configRequest.setHttpsProxy("");
//        configRequest.setReverseProxy("");
//        configRequest.setSocksProxy("");
//        configRequest.setTimeoutMs("");
//        configRequest.setUsage("");
//        return R.succeed(configRequest);
//    }
}
