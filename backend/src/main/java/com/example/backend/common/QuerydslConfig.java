package com.example.backend.common;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 설정 클래스
 * - JPAQueryFactory 빈을 등록하여 QueryDSL 사용을 위한 설정
 * - EntityManager를 주입받아 JPAQueryFactory 생성
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager em;

    /**
     * JPAQueryFactory 빈 등록
     * - QueryDSL을 사용하기 위한 핵심 빈
     * - EntityManager를 주입받아 생성
     * @return JPAQueryFactory 인스턴스
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
}

