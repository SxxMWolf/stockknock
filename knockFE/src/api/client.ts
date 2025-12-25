import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden) 오류 시 처리
    // 인증 관련 API에서 400 오류는 토큰이 유효하지 않을 때 발생할 수 있음
    if (error.response?.status === 400 || 
        error.response?.status === 401 || 
        error.response?.status === 403) {
      // 인증 API는 제외 (로그인/회원가입 페이지에서 발생할 수 있음)
      const isAuthEndpoint = error.config?.url?.includes('/auth/login') || 
                             error.config?.url?.includes('/auth/register');
      
      // 인증이 필요한 엔드포인트에서 400/401/403 오류 발생 시 로그아웃 처리
      if (!isAuthEndpoint) {
        // watchlist, portfolio 등 비즈니스 로직 오류가 발생할 수 있는 엔드포인트는 400 오류 시 제외
        const isBusinessLogicEndpoint = error.config?.url?.includes('/watchlist') ||
                                       error.config?.url?.includes('/portfolio') ||
                                       error.config?.url?.includes('/alerts');
        
        // 토큰 관련 오류인 경우 (에러 메시지 확인)
        const errorMessage = error.response?.data?.error || error.response?.data?.message || '';
        const isTokenError = errorMessage.toLowerCase().includes('token') || 
                            errorMessage.toLowerCase().includes('jwt') ||
                            errorMessage.toLowerCase().includes('expired') ||
                            errorMessage.toLowerCase().includes('unauthorized') ||
                            errorMessage.toLowerCase().includes('인증');
        
        // 401/403은 항상 토큰 오류로 간주
        // 400 오류는 비즈니스 로직 엔드포인트가 아니고 토큰 오류 메시지가 있을 때만 로그아웃
        const shouldLogout = error.response?.status === 401 || 
                            error.response?.status === 403 ||
                            (error.response?.status === 400 && !isBusinessLogicEndpoint && isTokenError);
        
        if (shouldLogout) {
          console.warn('인증 오류 발생. 로그아웃 처리합니다.', error.response?.status, errorMessage);
          localStorage.removeItem('token');
          localStorage.removeItem('userId');
          localStorage.removeItem('username');
          localStorage.removeItem('email');
          localStorage.removeItem('name');
          // 현재 경로가 로그인 페이지가 아닐 때만 리다이렉트
          if (window.location.pathname !== '/login') {
            window.location.href = '/login';
          }
        }
      }
    }
    return Promise.reject(error);
  }
);

