package com.shengding.shengdingllm.vo;

import lombok.Data;

@Data
public class ChoicesResponse {
    private int index = 0;
    private ResponseMessage delta;
    private String finish_reason;

    public ChoicesResponse(String content) {
        this.delta = new ResponseMessage(content);
    }
    public ChoicesResponse(String role,String content) {
        this.delta = new ResponseMessage(role,content);
    }

    public ChoicesResponse(Boolean isStop) {
        this.finish_reason = isStop ? "stop" : null;
    }
}
