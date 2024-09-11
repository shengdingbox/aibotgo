package com.shengding.shengdingllm.utils;


import com.shengding.shengdingllm.vo.AiModelVo;
import com.shengding.shengdingllm.vo.RequestRateLimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalCache {
    public static final Map<String, String> CONFIGS = new ConcurrentHashMap<>();

    public static RequestRateLimit TEXT_RATE_LIMIT_CONFIG;

    public static RequestRateLimit IMAGE_RATE_LIMIT_CONFIG;

    public static Map<Long, AiModelVo> MODEL_ID_TO_OBJ = new ConcurrentHashMap<>();
}
