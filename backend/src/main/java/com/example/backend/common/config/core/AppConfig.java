package com.example.backend.common.config.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 애플리케이션 설정을 위한 구성 클래스
 * - 비밀번호 암호화 등 공통 빈 정의
 */
@Configuration
public class AppConfig {

    /**
     * 비밀번호 암호화를 위한 BCryptPasswordEncoder 빈 등록
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * REST API 호출을 위한 RestTemplate 빈 등록
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * JSON 처리를 위한 ObjectMapper 빈 등록
     * - Java 8 날짜/시간 타입 지원 모듈 추가
     * - LocalDateTime을 ISO 형식으로 직렬화
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // JavaTimeModule 설정
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // LocalDateTime을 ISO 형식으로 직렬화
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        
        objectMapper.registerModule(javaTimeModule);
        return objectMapper;
    }
} 