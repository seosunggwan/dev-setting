package com.example.backend.service;

import com.example.backend.security.service.UserService;
import com.example.backend.security.entity.UserEntity;
import com.example.backend.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * UserService SecurityContext 테스트
 * - SecurityContextHolder를 사용하는 서비스 메서드 테스트
 * - @WithMockUser를 통한 SecurityContext 모킹
 * - 서비스 레이어에서의 현재 인증된 사용자 정보 접근 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceSecurityTest {

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    @DisplayName("SecurityContext에서 현재 사용자 이메일 조회 성공")
    void SecurityContext에서_현재사용자_이메일_조회_성공() {
        // given - SecurityContextHolder에서 인증 정보 가져오기
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        
        // when & then
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("test@example.com");
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("SecurityContext에서 관리자 권한 확인 성공")
    void SecurityContext에서_관리자권한_확인_성공() {
        // given
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        
        // when & then
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("admin@example.com");
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    @DisplayName("SecurityContext를 통한 현재 사용자 엔티티 조회 시뮬레이션")
    void SecurityContext를_통한_현재사용자_엔티티_조회_시뮬레이션() {
        // given
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity mockUser = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .username("테스트사용자")
                .build();
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        // when
        Optional<UserEntity> foundUser = userRepository.findByEmail(currentUserEmail);

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getUsername()).isEqualTo("테스트사용자");
        assertThat(currentUserEmail).isEqualTo("test@example.com");
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER", "ADMIN"})
    @DisplayName("다중 권한을 가진 사용자의 SecurityContext 테스트")
    void 다중권한_사용자_SecurityContext_테스트() {
        // given
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        
        // when & then
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("test@example.com");
        assertThat(authentication.getAuthorities()).hasSize(2);
        
        // 권한 확인
        boolean hasUserRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"));
        boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        assertThat(hasUserRole).isTrue();
        assertThat(hasAdminRole).isTrue();
    }

    @Test
    @DisplayName("인증되지 않은 상태에서 SecurityContext 확인")
    void 인증되지_않은_상태에서_SecurityContext_확인() {
        // given - @WithMockUser 없음으로 인증되지 않은 상태
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        
        // when & then
        // Spring Test에서는 기본적으로 익명 사용자로 설정될 수 있음
        if (authentication != null) {
            assertThat(authentication.getName()).isEqualTo("anonymousUser");
        } else {
            assertThat(authentication).isNull();
        }
    }

    /**
     * 실제 서비스에서 SecurityContext를 사용하는 메서드 예시
     * 이런 방식으로 서비스 메서드를 테스트할 수 있습니다.
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    @DisplayName("SecurityContext를 사용하는 서비스 메서드 테스트 예시")
    void SecurityContext를_사용하는_서비스메서드_테스트_예시() {
        // given
        String currentUserEmail = getCurrentUserEmail(); // 헬퍼 메서드
        
        // when & then
        assertThat(currentUserEmail).isEqualTo("test@example.com");
    }

    /**
     * SecurityContextHolder에서 현재 사용자 이메일을 가져오는 헬퍼 메서드
     * 실제 서비스에서 이런 방식으로 구현될 수 있습니다.
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}