package com.demo.upimesh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    @Value("${idempotency.ttl-seconds}")
    private long ttlSeconds;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically claims a packet hash in Redis.
     * Returns true only for the first caller within the TTL window.
     */
    public boolean claim(String packetHash) {
        String key = "idempotency:" + packetHash;

        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(
                        key,
                        "PROCESSING",
                        Duration.ofSeconds(ttlSeconds)
                );

        return Boolean.TRUE.equals(claimed);
    }



    public int size() {
        Set<String> keys = redisTemplate.keys("idempotency:*");
        return keys != null ? keys.size() : 0;
    }

    public void markSettled(String packetHash) {
        String key = "idempotency:" + packetHash;

        redisTemplate.opsForValue().set(
                key,
                "SETTLED",
                Duration.ofSeconds(ttlSeconds)
        );
    }

    public void release(String packetHash) {
        redisTemplate.delete("idempotency:" + packetHash);
    }

    /** Test/demo helper. */
    public void clear() {
        Set<String> keys = redisTemplate.keys("idempotency:*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}