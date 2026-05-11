package com.ecommerce.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties properties = new Properties();
    private static JedisPool jedisPool;

    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application properties", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static synchronized JedisPool getRedisPool() {
        if (jedisPool == null) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(128);
            poolConfig.setMaxIdle(64);
            poolConfig.setMinIdle(16);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
            poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
            poolConfig.setNumTestsPerEvictionRun(3);
            poolConfig.setBlockWhenExhausted(true);

            String host = getProperty("redis.host", "localhost");
            int port = Integer.parseInt(getProperty("redis.port", "6379"));
            int timeout = Integer.parseInt(getProperty("redis.timeout", "3000"));

            jedisPool = new JedisPool(poolConfig, host, port, timeout);
        }
        return jedisPool;
    }

    public static Jedis getRedisConnection() {
        return getRedisPool().getResource();
    }

    public static void closeRedisPool() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}

class Duration {
    public static DurationBuilder ofSeconds(long seconds) {
        return new DurationBuilder(seconds * 1000);
    }

    static class DurationBuilder {
        private final long millis;

        DurationBuilder(long millis) {
            this.millis = millis;
        }

        long toMillis() {
            return millis;
        }
    }
}