import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import './EmailChange.css';

const EmailChange: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [newEmail, setNewEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [password, setPassword] = useState('');
  const [step, setStep] = useState<'email' | 'verify'>('email');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [codeSent, setCodeSent] = useState(false);

  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (!newEmail.trim()) {
        setError('새 이메일을 입력해주세요.');
        setLoading(false);
        return;
      }

      await authAPI.sendEmailVerificationCode(newEmail);
      setCodeSent(true);
      setStep('verify');
    } catch (err: any) {
      setError(err.response?.data?.error || '인증 코드 전송에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleChangeEmail = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (!verificationCode.trim()) {
        setError('인증 코드를 입력해주세요.');
        setLoading(false);
        return;
      }

      if (!password.trim()) {
        setError('비밀번호를 입력해주세요.');
        setLoading(false);
        return;
      }

      await authAPI.changeEmail(newEmail, verificationCode, password);
      alert('이메일이 변경되었습니다. 다시 로그인해주세요.');
      logout();
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.error || '이메일 변경에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="email-change-container">
      <div className="email-change-box">
        <h2>이메일 변경</h2>
        <p className="current-email">현재 이메일: {user?.email}</p>

        {step === 'email' && (
          <form onSubmit={handleSendCode}>
            <div className="form-group">
              <label>새 이메일</label>
              <input
                type="email"
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                required
                placeholder="새 이메일을 입력하세요"
                disabled={loading}
              />
            </div>

            {error && <div className="error-message">{error}</div>}

            <button type="submit" disabled={loading}>
              {loading ? '전송 중...' : '인증 코드 전송'}
            </button>

            <button
              type="button"
              className="cancel-button"
              onClick={() => navigate('/dashboard')}
            >
              취소
            </button>
          </form>
        )}

        {step === 'verify' && (
          <form onSubmit={handleChangeEmail}>
            <div className="form-group">
              <label>새 이메일</label>
              <input
                type="email"
                value={newEmail}
                disabled
                className="disabled-input"
              />
              {codeSent && (
                <p className="code-sent-message">
                  인증 코드가 {newEmail}로 전송되었습니다.
                </p>
              )}
            </div>

            <div className="form-group">
              <label>인증 코드</label>
              <input
                type="text"
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value)}
                required
                placeholder="6자리 인증 코드를 입력하세요"
                maxLength={6}
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label>현재 비밀번호</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="보안을 위해 현재 비밀번호를 입력하세요"
                disabled={loading}
              />
            </div>

            {error && <div className="error-message">{error}</div>}

            <button type="submit" disabled={loading}>
              {loading ? '변경 중...' : '이메일 변경'}
            </button>

            <button
              type="button"
              className="cancel-button"
              onClick={() => {
                setStep('email');
                setVerificationCode('');
                setPassword('');
                setError('');
              }}
            >
              뒤로
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

export default EmailChange;



