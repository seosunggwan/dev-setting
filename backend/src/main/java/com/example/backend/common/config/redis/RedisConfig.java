package com.example.backend.common.config.redis;

import com.example.backend.chat.service.RedisPubSubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * Redis Connection Factory (Pub/Sub 및 일반 Redis 용도)
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host); // Docker 환경이면 "redis"로 설정해야 함
        redisConfig.setPort(port);
        redisConfig.setPassword(password); // 비밀번호 설정 (없으면 빈 문자열 가능)

        return new LettuceConnectionFactory(redisConfig);
    }

    /**
     * Object 타입을 지원하는 RedisTemplate (AOP 캐싱용)
     */
    @Bean
    public RedisTemplate<String, Object> redisObjectTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.registerModule(new JavaTimeModule());

        // Serializer 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        return redisTemplate;
    }

    /**
     * 기본 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 한글 깨짐 방지를 위한 Serializer 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    /**
     * 채팅 메시지를 위한 StringRedisTemplate
     */
    @Bean(name = "chatPubSub")
    public StringRedisTemplate chatPubSubTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    /**
     * Redis 메시지 리스너 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic("chat"));
        return container;
    }

    /**
     * Redis 메시지를 수신하는 리스너 어댑터
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisPubSubService redisPubSubService) {
        return new MessageListenerAdapter(redisPubSubService, "onMessage");
    }
}
