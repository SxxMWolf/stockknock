import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { authAPI } from '../api/auth';
import type { AuthResponse } from '../api/auth';

interface AuthContextType {
  isAuthenticated: boolean;
  user: AuthResponse | null;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string, nickname: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState<AuthResponse | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    if (token && userId) {
      setIsAuthenticated(true);
      setUser({
        token,
        userId: parseInt(userId),
        username: '',
        email: '',
        name: '',
      });
    }
  }, []);

  const login = async (username: string, password: string) => {
    const response = await authAPI.login({ username, password });
    setUser(response);
    setIsAuthenticated(true);
  };

  const register = async (username: string, email: string, password: string, nickname: string) => {
    const response = await authAPI.register({ username, email, password, nickname });
    setUser(response);
    setIsAuthenticated(true);
  };

  const logout = () => {
    authAPI.logout();
    setUser(null);
    setIsAuthenticated(false);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

