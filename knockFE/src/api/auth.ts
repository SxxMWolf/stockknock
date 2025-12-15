import { apiClient } from './client';

export interface AuthRequest {
  username?: string; // 로그인 시 필수, 회원가입 시 필수
  email: string;
  password: string;
  nickname?: string; // 회원가입 시에만 사용
}

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  name: string;
  userId: number;
}

export const authAPI = {
  login: async (credentials: AuthRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/login', credentials);
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('userId', response.data.userId.toString());
    }
    return response.data;
  },

  register: async (credentials: AuthRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/register', credentials);
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('userId', response.data.userId.toString());
    }
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
  },

  // 이메일 변경 인증 코드 전송
  sendEmailVerificationCode: async (newEmail: string): Promise<{ message: string }> => {
    const response = await apiClient.post<{ message: string }>('/auth/email/verification-code', {
      newEmail,
    });
    return response.data;
  },

  // 이메일 변경
  changeEmail: async (newEmail: string, verificationCode: string, password: string): Promise<{ message: string }> => {
    const response = await apiClient.put<{ message: string }>('/auth/email', {
      newEmail,
      verificationCode,
      password,
    });
    return response.data;
  },
};

