package com.dog.gatewaysystem.Utils;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import java.util.Collections;

// 令牌桶工具类
@Component
public class TokenBucketUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisScript<Long> redisScript;

    @Value("${token-bucket.rate}")
    private Long rate;
    @Value("${token-bucket.capacity}")
    private Long capacity;
    @Value("${token-bucket.expire}")
    private Long expire;

    public boolean tryAcquireToken(String key) {
        Long now = System.currentTimeMillis();

        Long allow = redisTemplate.execute(redisScript, Collections.singletonList(key), String.valueOf(rate), String.valueOf(capacity), String.valueOf(now), String.valueOf(expire));
        return allow == 1;
    }
}
