import axiosInstance from "../utils/axios";
import { redirectToLogin } from "../utils/auth";

/**
 * 📌 사용자가 참여한 채팅방 목록을 가져오는 함수
 * - GET 메서드로 요청
 * - 토큰 만료 시 자동으로 재발급 처리 (axios 인터셉터 활용)
 * - 페이지네이션 지원
 */
const fetchMyChatRooms = async (
  page = 0,
  size = 10,
  navigate,
  location
) => {
  try {
    console.log("내 채팅방 목록 조회 시작");

    const params = { page, size };

    console.log("API 요청 파라미터:", params);

    const response = await axiosInstance.get("/api/chat/room/my/list", {
      params: params,
    });

    console.log("내 채팅방 목록 API 응답:", response.data);
    return response.data;
  } catch (error) {
    console.error("내 채팅방 목록 조회 중 오류 발생:", error);

    // 인증 오류가 해결되지 않은 경우 (토큰 재발급 후에도 실패)
    if (error.response?.status === 401) {
      redirectToLogin(navigate, location.pathname);
      return null;
    }

    return {
      rooms: [],
      pageInfo: { page: 0, size: 10, totalPages: 0, total: 0 },
    };
  }
};

export default fetchMyChatRooms;
