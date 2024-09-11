package com.shengding.shengdingllm.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResponseMessage {
    private String role;
    private String content;

    public ResponseMessage(String content) {
        this.content = content;
    }
    public ResponseMessage(String role,String content) {
        this.role = role;
        this.content = content;
    }


}
