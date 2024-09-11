package com.shengding.shengdingllm.vo;

import com.shengding.shengdingllm.api.request.Message;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

@Data
public class ChoicesResponse {
    private int index = 0;
    private ResponseMessage delta;
    private String finish_reason;

    public ChoicesResponse(String content) {
        this.delta = new ResponseMessage(content);
    }

    public ChoicesResponse(Boolean isStop) {
        this.finish_reason = isStop ? "stop" : null;
    }
}
