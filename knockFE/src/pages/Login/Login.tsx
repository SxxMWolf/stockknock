import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './Login.css';

const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [isRegister, setIsRegister] = useState(false);
  const [error, setError] = useState('');
  const { login, register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      if (isRegister) {
        if (!username.trim()) {
          setError('아이디를 입력해주세요.');
          return;
        }
        if (!email.trim()) {
          setError('이메일을 입력해주세요.');
          return;
        }
        if (!nickname.trim()) {
          setError('닉네임을 입력해주세요.');
          return;
        }
        await register(username, email, password, nickname);
      } else {
        if (!username.trim()) {
          setError('아이디를 입력해주세요.');
          return;
        }
        await login(username, password);
      }
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || (isRegister ? '회원가입에 실패했습니다.' : '로그인에 실패했습니다.'));
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>{isRegister ? '회원가입' : '로그인'}</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>아이디</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              placeholder="아이디를 입력하세요"
            />
          </div>
          {isRegister && (
            <>
              <div className="form-group">
                <label>이메일</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder="이메일을 입력하세요"
                />
              </div>
              <div className="form-group">
                <label>닉네임</label>
                <input
                  type="text"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  required
                  placeholder="닉네임을 입력하세요"
                />
              </div>
            </>
          )}
          <div className="form-group">
            <label>비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="비밀번호를 입력하세요"
            />
          </div>
          {error && <div className="error">{error}</div>}
          <button type="submit">{isRegister ? '회원가입' : '로그인'}</button>
        </form>
        <p onClick={() => setIsRegister(!isRegister)}>
          {isRegister ? '이미 계정이 있으신가요? 로그인' : '계정이 없으신가요? 회원가입'}
        </p>
      </div>
    </div>
  );
};

export default Login;

