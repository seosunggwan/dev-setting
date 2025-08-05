import axios from "axios";
import fetchReissue from "./fetchReissue";

// 환경에 따른 API 베이스 URL 설정
const getBaseURL = () => {
  // 환경변수가 있으면 사용, 없으면 개발 환경용 프록시 사용
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  if (apiBaseUrl) {
    return apiBaseUrl + '/api';  // 환경변수에 /api 추가
  }
  
  // 개발 환경에서는 Vite 프록시를 사용하도록 상대 경로 설정
  if (typeof window !== 'undefined' && window.location.origin) {
    return `${window.location.origin}/api`;
  }
  return "/api";
};

const axiosInstance = axios.create({
  baseURL: getBaseURL(),
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000, // 10초 타임아웃
});

axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("access_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log(`요청 인터셉터: ${config.url}에 토큰 추가됨`);
    } else {
      console.log("토큰이 없습니다.");
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    console.log(`응답 에러: ${error.config.url}, 상태: ${error.response?.status}`);
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      console.log("🔄 401 에러 발생, 토큰 갱신 시도 중...");
      const success = await fetchReissue();
      if (success) {
        console.log("✅ 토큰 갱신 성공, 요청 재시도");
        const token = localStorage.getItem("access_token");
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return axiosInstance(originalRequest);
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
