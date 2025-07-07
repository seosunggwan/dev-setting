import axios from "axios";
import fetchReissue from "./fetchReissue";

// 환경에 따른 API 베이스 URL 설정
const getBaseURL = () => {
  if (import.meta.env.MODE === 'development') {
    return "http://localhost:8080/api";
  } else if (import.meta.env.MODE === 'production') {
    // 운영 환경에서는 EC2 IP 또는 도메인 사용
    return import.meta.env.VITE_API_BASE_URL || "http://your-ec2-ip:8080/api";
  }
  return "http://localhost:8080/api";
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
      console.log("요청 헤더:", config.headers);
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
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      console.log("🔄 401 에러 발생, 토큰 갱신 시도 중...");
      const success = await fetchReissue();
      if (success) {
        const token = localStorage.getItem("access_token");
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return axiosInstance(originalRequest);
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
