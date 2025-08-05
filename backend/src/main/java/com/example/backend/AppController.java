package com.example.backend;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.board.entity.Board;
import com.example.backend.board.repository.BoardRepository;

@RestController
public class AppController {

  private final BoardRepository boardRepository;

  public AppController(BoardRepository boardRepository) {
    this.boardRepository = boardRepository;
  }

  @GetMapping("healthservertest")
  public ResponseEntity<String> healthCheck() {
    try {
      String privateIp = InetAddress.getLocalHost().getHostAddress();
      return ResponseEntity.ok().body(privateIp + " / Success Health Check");
    } catch (UnknownHostException e) {
      return ResponseEntity.ok().body("Unknown Host");
    }
  }

  @GetMapping("boardservertest")
  public ResponseEntity<List<Board>> getBoards() {
    // 더미 데이터 생성을 위한 게시글 저장 로직
    // 기존 Board 엔티티는 Builder 패턴을 사용하므로 수정
    Board board = Board.builder()
        .title("게시글 제목")
        .content("게시글 내용")
        .viewCount(0)
        .likeCount(0)
        .build();
    
    boardRepository.save(board);

    // 게시글 조회 테스트
    List<Board> boards = boardRepository.findAll();
    return ResponseEntity.ok().body(boards);
  }
} 