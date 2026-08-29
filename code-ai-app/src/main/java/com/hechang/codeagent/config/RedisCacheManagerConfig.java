package com.hechang.codeagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置，如果不配置的话，使用缓存注解时可能会报错
 * 这个配置的几个关键点:
 *      1.序列化器选择：StringRedisSerializer 用于序列化 key，
 *          确保Redis 中的key是可读的字符串；GenericJackson2JsonRedisSerializer 用于序列化Value，支持复杂对象的序列化和反序列化。
 *      2.时间类型持：注册J avaTimeModule 来持 Java8 时间类型 LocalDateTime
 *      3.差异化配置：既提供了默认配置，文为特定的缓存区域设置不同的过期时间
 */
@Configuration
public class RedisCacheManagerConfig {
    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public CacheManager cacheManager() {
        //配置 ObjectMapper 支持 Java8 时间类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        //默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))  // 默认 30 分钟过期
                .disableCachingNullValues() // 禁用 null 值缓存
                .serializeKeysWith(RedisSerializationContext.SerializationPair // key 使用 String 序列化器
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair // value 使用 JSON 序列化器（支持复杂对象）
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));
        //要注意，如果对value进行JSON序列化，可能会出现无法反序列化的情况，因为Redis中并没有存储 Java类的信息，不知道要反序列化成哪个类，就会报错。所以我们可以先注释掉这些代码:
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                // 针对 good_app_page 配置5分钟过期
                .withCacheConfiguration("good_app_page",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}
