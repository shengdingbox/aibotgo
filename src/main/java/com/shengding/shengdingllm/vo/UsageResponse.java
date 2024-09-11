package com.shengding.shengdingllm.vo;

import lombok.Data;

@Data
public class UsageResponse {

    private int prompt_tokens;
    private int completion_tokens;
    private int total_tokens;
}
