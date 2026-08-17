package com.demo.upimesh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency cache. In production this would be Redis with SETNX +
 * TTL — exactly the same semantics, just distributed across instances.
 *
 * The contract:
 *   - claim(hash) returns true on first call, false on every call after that
 *     (within the TTL window)
 *   - the operation is atomic — even if 100 threads call claim(hash) at the
 *     same instant, exactly one returns true
 *
 * This is what kills the "three bridges deliver simultaneously" problem.
 * ConcurrentHashMap.putIfAbsent is the JVM-local equivalent of Redis SETNX.
 */
@Service
public class IdempotencyService {
    private final StringRedisTemplate redisTemplate;


    private final Map<String, Instant> seen = new ConcurrentHashMap<>();

    @Value("${idempotency.ttl-seconds}")
    private long ttlSeconds;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Try to claim a hash. Returns true if this caller is the first; false if
     * someone else already claimed it (i.e. the packet is a duplicate).
     */
    public boolean claim(String packetHash) {
        String key = "idempotency:" + packetHash;

        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(key, "PROCESSING", Duration.ofSeconds(ttlSeconds));

        return Boolean.TRUE.equals(claimed);
    }

    public int size() {
        Set<String> keys = redisTemplate.keys("idempotency:*");
        return keys !=  null ? keys.size() : 0;
    }

    /** Test/demo helper. */
    public void clear() {
        Set<String> keys = redisTemplate.keys("idempotency:*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
