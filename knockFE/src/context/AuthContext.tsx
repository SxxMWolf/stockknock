import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authAPI, AuthResponse } from '../api/auth';

interface AuthContextType {
  isAuthenticated: boolean;
  user: AuthResponse | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
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
        email: '',
        name: '',
      });
    }
  }, []);

  const login = async (email: string, password: string) => {
    const response = await authAPI.login({ email, password });
    setUser(response);
    setIsAuthenticated(true);
  };

  const register = async (email: string, password: string) => {
    const response = await authAPI.register({ email, password });
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

