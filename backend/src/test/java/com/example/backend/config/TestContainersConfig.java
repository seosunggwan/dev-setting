package com.example.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

/**
 * TestContainers 기반 테스트 환경 설정
 * - MySQL 8.0.33 컨테이너로 실제 DB 환경 구성
 * - Redis 7.0 컨테이너로 실제 캐시 환경 구성
 * - 운영 환경과 동일한 조건에서 테스트 실행
 */
@TestConfiguration
public class TestContainersConfig {

    private static final MySQLContainer<?> mysqlContainer;
    private static final GenericContainer<?> redisContainer;

    static {
        // MySQL 8.0.33 컨테이너 시작
        mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.33"))
                .withDatabaseName("portfolio_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withInitScript("test-init.sql");  // 초기화 스크립트 (선택사항)
        
        // Redis 7.0 컨테이너 시작  
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
                .withExposedPorts(6379);
        
        // 컨테이너 시작
        mysqlContainer.start();
        redisContainer.start();
        
        // JVM 종료 시 컨테이너 정리
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mysqlContainer.stop();
            redisContainer.stop();
        }));
    }

    /**
     * TestContainers MySQL을 사용하는 DataSource 설정
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(mysqlContainer.getJdbcUrl())
                .username(mysqlContainer.getUsername())
                .password(mysqlContainer.getPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    /**
     * TestContainers Redis를 사용하는 RedisConnectionFactory 설정
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                redisContainer.getHost(),
                redisContainer.getMappedPort(6379)
        );
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Redis 테스트용 RedisTemplate 설정
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * MySQL 컨테이너 정보 제공 (테스트에서 필요한 경우)
     */
    @Bean
    public MySQLContainer<?> mysqlContainer() {
        return mysqlContainer;
    }

    /**
     * Redis 컨테이너 정보 제공 (테스트에서 필요한 경우)
     */
    @Bean
    public GenericContainer<?> redisContainer() {
        return redisContainer;
    }
} 