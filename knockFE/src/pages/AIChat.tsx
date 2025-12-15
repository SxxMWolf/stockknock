import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { aiAPI } from '../api/ai';
import { useAuth } from '../context/AuthContext';
import './AIChat.css';

const AIChat: React.FC = () => {
  const { user } = useAuth();
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<Array<{ role: 'user' | 'ai'; content: string }>>([]);

  const mutation = useMutation({
    mutationFn: (q: string) => aiAPI.chat({ 
      question: q,
      user_id: user?.userId || 0,
      conversationType: 'general'
    }),
    onSuccess: (data) => {
      setMessages(prev => [
        ...prev,
        { role: 'ai', content: data.response }
      ]);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim() || !user?.userId) return;

    setMessages(prev => [...prev, { role: 'user', content: question }]);
    mutation.mutate(question);
    setQuestion('');
  };

  return (
    <div className="ai-chat">
      <h1>AI 분석가와 대화하기</h1>
      <div className="chat-container">
        <div className="messages">
          {messages.length === 0 && (
            <div className="welcome">
              <p>주식 투자에 대한 질문을 해보세요!</p>
              <p>예: "애플 주식의 전망은 어때요?"</p>
            </div>
          )}
          {messages.map((msg, idx) => (
            <div key={idx} className={`message ${msg.role}`}>
              <div className="message-content">
                {msg.content}
              </div>
            </div>
          ))}
          {mutation.isPending && (
            <div className="message ai">
              <div className="message-content">분석 중...</div>
            </div>
          )}
        </div>
        <form onSubmit={handleSubmit} className="chat-input">
          <input
            type="text"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="질문을 입력하세요..."
            disabled={mutation.isPending}
          />
          <button type="submit" disabled={mutation.isPending || !question.trim()}>
            전송
          </button>
        </form>
      </div>
    </div>
  );
};

export default AIChat;

