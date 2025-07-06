package com.example.backend.board.service;

import com.example.backend.annotation.IntegrationTest;
import com.example.backend.board.dto.BoardDto;
import com.example.backend.board.dto.PagedBoardsDto;
import com.example.backend.board.entity.Board;
import com.example.backend.board.repository.BoardRepository;
import com.example.backend.security.entity.UserEntity;
import com.example.backend.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * BoardService 통합 테스트
 * - H2 인메모리 데이터베이스 사용
 * - 실제 DB 트랜잭션 테스트
 * - 서비스 로직 검증
 */
@IntegrationTest
@Transactional
@Rollback
class BoardServiceTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("게시글 생성 테스트")
    void createBoardTest() {
        // given
        BoardDto.CreateRequest request = BoardDto.CreateRequest.builder()
                .title("테스트 게시글")
                .content("테스트 내용입니다. 10자 이상 입력해야 합니다.")
                .build();

        // when
        BoardDto.Response response = boardService.create(request, testUser.getEmail());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("테스트 게시글");
        assertThat(response.getContent()).isEqualTo("테스트 내용입니다. 10자 이상 입력해야 합니다.");
        assertThat(response.getAuthorName()).isEqualTo(testUser.getUsername());
        assertThat(response.getViewCount()).isEqualTo(0);
        assertThat(response.getLikeCount()).isEqualTo(0);
        assertThat(response.isAuthor()).isTrue();
    }

    @Test
    @DisplayName("게시글 조회 테스트")
    void getBoardTest() {
        // given
        Board board = Board.builder()
                .title("조회 테스트 게시글")
                .content("조회 테스트 내용입니다. 10자 이상 입력해야 합니다.")
                .author(testUser)
                .viewCount(0)
                .likeCount(0)
                .build();
        Board savedBoard = boardRepository.save(board);

        // when
        BoardDto.Response response = boardService.getBoard(savedBoard.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedBoard.getId());
        assertThat(response.getTitle()).isEqualTo("조회 테스트 게시글");
        assertThat(response.getViewCount()).isEqualTo(1); // 조회수 증가 확인
        assertThat(response.getAuthorName()).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("게시글 전체 목록 조회 테스트")
    void getAllBoardsTest() {
        // given
        for (int i = 1; i <= 5; i++) {
            Board board = Board.builder()
                    .title("테스트 게시글 " + i)
                    .content("테스트 내용입니다. 10자 이상 입력해야 합니다. " + i)
                    .author(testUser)
                    .viewCount(0)
                    .likeCount(0)
                    .build();
            boardRepository.save(board);
        }

        // when
        List<BoardDto.ListResponse> boards = boardService.getAllBoards();

        // then
        assertThat(boards).isNotNull();
        assertThat(boards).hasSize(5);
        assertThat(boards.get(0).getTitle()).isEqualTo("테스트 게시글 5"); // 최신 순 정렬
    }

    @Test
    @DisplayName("페이지네이션 게시글 목록 조회 테스트")
    void getBoardsWithPagingTest() {
        // given
        for (int i = 1; i <= 15; i++) {
            Board board = Board.builder()
                    .title("테스트 게시글 " + i)
                    .content("테스트 내용입니다. 10자 이상 입력해야 합니다. " + i)
                    .author(testUser)
                    .viewCount(0)
                    .likeCount(0)
                    .build();
            boardRepository.save(board);
        }

        // when
        PagedBoardsDto result = boardService.getBoardsWithPaging(0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getBoards()).hasSize(10);
        assertThat(result.getPageInfo().getTotal()).isEqualTo(15);
        assertThat(result.getPageInfo().getTotalPages()).isEqualTo(2);
        assertThat(result.getPageInfo().isFirst()).isTrue();
        assertThat(result.getPageInfo().isHasNext()).isTrue();
    }

    @Test
    @DisplayName("게시글 수정 테스트")
    void updateBoardTest() {
        // given
        Board board = Board.builder()
                .title("수정 전 제목")
                .content("수정 전 내용입니다. 10자 이상 입력해야 합니다.")
                .author(testUser)
                .viewCount(0)
                .likeCount(0)
                .build();
        Board savedBoard = boardRepository.save(board);

        BoardDto.UpdateRequest request = BoardDto.UpdateRequest.builder()
                .title("수정 후 제목")
                .content("수정 후 내용입니다. 10자 이상 입력해야 합니다.")
                .build();

        // when
        BoardDto.Response response = boardService.update(savedBoard.getId(), request, testUser.getEmail());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedBoard.getId());
        assertThat(response.getTitle()).isEqualTo("수정 후 제목");
        assertThat(response.getContent()).isEqualTo("수정 후 내용입니다. 10자 이상 입력해야 합니다.");
        assertThat(response.getAuthorName()).isEqualTo(testUser.getUsername());
        assertThat(response.isAuthor()).isTrue();
    }

    @Test
    @DisplayName("게시글 삭제 테스트")
    void deleteBoardTest() {
        // given
        Board board = Board.builder()
                .title("삭제 테스트 게시글")
                .content("삭제 테스트 내용입니다. 10자 이상 입력해야 합니다.")
                .author(testUser)
                .viewCount(0)
                .likeCount(0)
                .build();
        Board savedBoard = boardRepository.save(board);

        // when
        boardService.delete(savedBoard.getId(), testUser.getEmail());

        // then
        Optional<Board> deletedBoard = boardRepository.findById(savedBoard.getId());
        assertThat(deletedBoard).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외 발생 테스트")
    void getBoardNotFoundExceptionTest() {
        // given
        Long nonExistentId = 999L;

        // when & then
        assertThatThrownBy(() -> boardService.getBoard(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("게시글 키워드 검색 테스트")
    void searchBoardsByKeywordTest() {
        // given
        Board board1 = Board.builder()
                .title("Spring Boot 튜토리얼")
                .content("Spring Boot 학습 내용입니다. 10자 이상 입력해야 합니다.")
                .author(testUser)
                .viewCount(0)
                .likeCount(0)
                .build();
        
        Board board2 = Board.builder()
                .title("Java 기초")
                .content("Java 프로그래밍 기초입니다. 10자 이상 입력해야 합니다.")
                .author(testUser)
                .viewCount(0)
                .likeCount(0)
                .build();
        
        boardRepository.save(board1);
        boardRepository.save(board2);

        // when
        PagedBoardsDto result = boardService.searchBoardsByKeyword("Spring", 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getBoards()).hasSize(1);
        assertThat(result.getBoards().get(0).getTitle()).contains("Spring");
    }

    @Test
    @DisplayName("작성자 이름으로 게시글 검색 테스트")
    void searchBoardsByAuthorTest() {
        // given
        Board board = Board.builder()
                .title("테스트 게시글")
                .content("테스트 내용입니다. 10자 이상 입력해야 합니다.")
                .author(testUser)
                .viewCount(0)
                .likeCount(0)
                .build();
        boardRepository.save(board);

        // when
        PagedBoardsDto result = boardService.searchBoardsByAuthor(testUser.getUsername(), 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getBoards()).hasSize(1);
        assertThat(result.getBoards().get(0).getAuthorName()).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("권한 없는 사용자의 게시글 수정 시 예외 발생 테스트")
    void updateBoardUnauthorizedTest() {
        // given
        UserEntity anotherUser = UserEntity.builder()
                .username("anotheruser")
                .email("another@example.com")
                .password("password123")
                .build();
        anotherUser = userRepository.save(anotherUser);

        Board board = Board.builder()
                .title("다른 사용자의 게시글")
                .content("다른 사용자가 작성한 게시글입니다. 10자 이상 입력해야 합니다.")
                .author(anotherUser)
                .viewCount(0)
                .likeCount(0)
                .build();
        Board savedBoard = boardRepository.save(board);

        BoardDto.UpdateRequest request = BoardDto.UpdateRequest.builder()
                .title("수정 시도")
                .content("수정 시도 내용입니다. 10자 이상 입력해야 합니다.")
                .build();

        // when & then
        assertThatThrownBy(() -> boardService.update(savedBoard.getId(), request, testUser.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("게시글 수정 권한이 없습니다");
    }

    @Test
    @DisplayName("권한 없는 사용자의 게시글 삭제 시 예외 발생 테스트")
    void deleteBoardUnauthorizedTest() {
        // given
        UserEntity anotherUser = UserEntity.builder()
                .username("anotheruser")
                .email("another@example.com")
                .password("password123")
                .build();
        anotherUser = userRepository.save(anotherUser);

        Board board = Board.builder()
                .title("다른 사용자의 게시글")
                .content("다른 사용자가 작성한 게시글입니다. 10자 이상 입력해야 합니다.")
                .author(anotherUser)
                .viewCount(0)
                .likeCount(0)
                .build();
        Board savedBoard = boardRepository.save(board);

        // when & then
        assertThatThrownBy(() -> boardService.delete(savedBoard.getId(), testUser.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("게시글 삭제 권한이 없습니다");
    }
} 