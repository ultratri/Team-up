package com.teamup.server.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis配置类 - 优化版本，添加缓存管理器和错误处理
 * 当 Redis 不可用时，缓存操作会优雅降级，不影响业务逻辑
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private GenericJackson2JsonRedisSerializer redisJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用String序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // 使用JSON序列化器（支持 JavaTime，例如 LocalDateTime）
        GenericJackson2JsonRedisSerializer jsonSerializer = redisJsonSerializer();
        
        // Key使用String序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Value使用JSON序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 配置缓存管理器，为不同的缓存设置不同的过期时间
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置：10分钟过期
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisJsonSerializer()))
                .disableCachingNullValues();
        
        // 为不同的缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 用户详情缓存：5分钟
        cacheConfigurations.put("userDetails", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 团队成员缓存：5分钟
        cacheConfigurations.put("teamMembers", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 团队信息缓存：10分钟
        cacheConfigurations.put("teams", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 项目信息缓存：10分钟
        cacheConfigurations.put("projects", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 人才墙缓存：5分钟（人才列表变化较频繁）
        cacheConfigurations.put("talentList", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 用户角色缓存：30分钟（角色变化不频繁）
        cacheConfigurations.put("userRoles", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 用户项目数量缓存：10分钟
        cacheConfigurations.put("userProjectCount", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
    
    /**
     * 配置缓存错误处理器
     * 当 Redis 连接失败时，优雅降级而不是抛出异常
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis 缓存读取失败，降级处理 - cache: {}, key: {}, error: {}", 
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis 缓存写入失败，降级处理 - cache: {}, key: {}, error: {}", 
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis 缓存清除失败，降级处理 - cache: {}, key: {}, error: {}", 
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis 缓存清空失败，降级处理 - cache: {}, error: {}", 
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
