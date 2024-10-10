package com.shengding.shengdingllm.api;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.api.request.ChatRequest;
import com.shengding.shengdingllm.exception.BaseException;
import com.shengding.shengdingllm.exception.ErrorCode;
import com.shengding.shengdingllm.service.ChatService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/auth")
@Slf4j
public class AuthController {

    @Autowired
    HttpServletRequest request;

    @Autowired
    private ChatService chatService;


    @PostMapping("/sendMsg")
    public String sendMessage(String type,String phone) {
        JSONObject jsonObject = new JSONObject();
        //{
        //	"action": "register",
        //	"phone": "18832878027",
        //	"country_code": "+86",
        //	"captcha_id": "a92156a088e54e8cad3fec9f8a7329c7",
        //	"lot_number": "8be688959f84466abccf34711d3c031a",
        //	"pass_token": "6b03b316adf96136f9ab591f8eb74870ca6af6157ee774df4118cf2ef2c5f3d3",
        //	"gen_time": "1726802621",
        //	"captcha_output": ""
        //}
        jsonObject.put("action", "register");
        jsonObject.put("phone", phone);
        jsonObject.put("country_code", "+86");
        jsonObject.put("captcha_id", "a92156a088e54e8cad3fec9f8a7329c7");
        jsonObject.put("lot_number", "8be688959f84466abccf34711d3c031a");
        jsonObject.put("pass_token", "6b03b316adf96136f9ab591f8eb74870ca6af6157ee774df4118cf2ef2c5f3d3");
        jsonObject.put("gen_time", "1726802621");
        jsonObject.put("captcha_output", "ZmoTX9s382_NtbBKxqjej6FWq3gDm0gjrzci-OgdZpqNvGmBQ8BiJZPEVqccwD_INmZphf9cYT6kEzQvnhrnkuq0DZWoUkAWWavvzh3MvxBFsCaFqxrQfg0WZwxG6ogQu45D4skenBrItb5Ij_J6GbeFBj9nGKjDTysMPU00WhA_Ul4ICJp9s24XizxhRid2Xw7GDsqzidZXcX_9ibYqe7VOb0KyPqh9oTBGPBDVcm7hA7CB8zo9W3gDjVtbpTMnRA2WlZkN1J1m8wQ4I1HJS-zLeMz0GBQxDeknAzNAg2FGepsUEOEh1SOCYiHNyttUKP-YOHjtuqfChY6sqmLOQejdpSgP2kpUHtodLUgueHV4fsexicdj8gKo-xdI97dcqCFNt8VCndX5cO_NWBDNqpRbZNmtQGGIpBxp0kkBXuguCsTVWiBNPOnxKNRGd8cj");

        HttpResponse execute = HttpRequest.post("https://kimi.moonshot.cn/api/user/sms/verify-code")
                .body(jsonObject.toJSONString())
                .header("Content-Type", "application/json")
                .execute();
        log.info(execute.body());
        return null;
    }

    @PostMapping("/login")
    public String config(String type,String phone,String verificationCode) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("phone", phone);
        jsonObject.put("country_code", "+86");
        jsonObject.put("verify_code", verificationCode);
        jsonObject.put("wx_user_id", null);
        HttpResponse execute = HttpRequest.post("https://kimi.moonshot.cn/api/user/register/trial")
                .body(jsonObject.toJSONString())
                .header("Content-Type", "application/json")
                .execute();
        log.info(execute.body());
        return null;
    }
}
