package com.shengding.shengdingllm.helper;

import com.shengding.shengdingllm.vo.RequestRateLimit;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
@Service
@Slf4j
public class RateLimitHelper {

    /**
     * 初始化一个ExpiringMap（配置过期时间、过期协议等）
     */
    public final static ExpiringMap<String, String> expireCacheMap = ExpiringMap.builder()
            // 设置最大值,添加第11个entry时，会导致第1个立马过期(即使没到过期时间)。默认 Integer.MAX_VALUE
            .maxSize(10)
            // 允许 Map 元素具有各自的到期时间，并允许更改到期时间。
            .variableExpiration()
            // 设置过期时间，如果key不设置过期时间，key永久有效。
            .expiration(10, TimeUnit.SECONDS)
            .asyncExpirationListener((key, value) -> {
                log.info("expireCacheMap key数据被删除了 -> key={}, value={}", key, value);
            })
            //设置 Map 的过期策略
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .build();

    /**
     * 按固定时间窗口计算请求次数
     *
     * @param requestTimesKey redis key
     * @param rateLimitConfig 请求频率限制配置
     * @return
     */
    public boolean checkRequestTimes(String requestTimesKey, RequestRateLimit rateLimitConfig) {
        int requestCountInTimeWindow = 0;
        String rateLimitVal = expireCacheMap.get(requestTimesKey);
        if (StringUtils.isNotBlank(rateLimitVal)) {
            requestCountInTimeWindow = Integer.parseInt(rateLimitVal);
        }
        if (requestCountInTimeWindow >= rateLimitConfig.getTimes()) {
            return false;
        }
        return true;
    }

    public void delete(String askingKey) {
        expireCacheMap.remove(askingKey);
    }
}
