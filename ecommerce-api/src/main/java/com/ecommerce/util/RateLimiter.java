package com.ecommerce.util;

import com.ecommerce.config.AppConfig;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private static final long WINDOW_SIZE = Long.parseLong(AppConfig.getProperty("rate.limit.window", "60")) * 1000; // Convert to ms
    private static final int MAX_REQUESTS = Integer.parseInt(AppConfig.getProperty("rate.limit.requests", "10"));
    private static final ConcurrentHashMap<String, RateLimitData> localCache = new ConcurrentHashMap<>();

    public static boolean allowRequest(String key) {
        if (!Boolean.parseBoolean(AppConfig.getProperty("rate.limit.enabled", "true"))) {
            return true;
        }

        try (Jedis jedis = com.ecommerce.config.AppConfig.getRedisConnection()) {
            String redisKey = "rate_limit:" + key;
            long now = System.currentTimeMillis();

            String countStr = jedis.get(redisKey);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;

            if (count >= MAX_REQUESTS) {
                return false;
            }

            jedis.incr(redisKey);
            jedis.expire(redisKey, (int) (WINDOW_SIZE / 1000));

            return true;
        } catch (Exception e) {
            // Fallback to local cache if Redis fails
            return checkLocalRateLimit(key);
        }
    }

    private static boolean checkLocalRateLimit(String key) {
        long now = System.currentTimeMillis();
        RateLimitData data = localCache.getOrDefault(key, new RateLimitData());

        if (now - data.windowStart > WINDOW_SIZE) {
            data.count = 0;
            data.windowStart = now;
        }

        if (data.count >= MAX_REQUESTS) {
            return false;
        }

        data.count++;
        localCache.put(key, data);
        return true;
    }

    private static class RateLimitData {
        int count = 0;
        long windowStart = System.currentTimeMillis();
    }
}