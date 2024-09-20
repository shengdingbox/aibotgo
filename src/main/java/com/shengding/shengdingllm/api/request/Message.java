package com.shengding.shengdingllm.api.request;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Message {
    private String role;
    private Object content;

    public Message(String content) {
        this.content = content;
    }

    public String getContent() {
        if (content instanceof String) {
            return (String) content;
        }
        return JSONObject.toJSONString(content);
    }

    public JSONArray getArrContent() {
        if (content instanceof JSONArray) {
            return JSONArray.parseArray((String) content);
        } else {
            return new JSONArray();
        }
    }
}
