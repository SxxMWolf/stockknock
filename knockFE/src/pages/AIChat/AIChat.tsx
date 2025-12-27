import React, { useState, useRef, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { aiAPI } from '../../api/ai';
import { useAuth } from '../../context/AuthContext';
import { parseMarkdown } from '../../utils/markdownParser';
import './AIChat.css';

const AIChat: React.FC = () => {
  const { user } = useAuth();
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<Array<{ role: 'user' | 'ai'; content: string }>>([]);
  const [showInfoCard, setShowInfoCard] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const exampleQuestions = [
    '애플 주식 전망은?',
    '삼성전자 지금 사도 될까?',
    '반도체 섹터 최근 이슈 요약',
    '단기 vs 장기 투자 전략 차이'
  ];

  const mutation = useMutation({
    mutationFn: (q: string) => aiAPI.chat({ 
      question: q,
      conversationType: 'general'
    }),
    onSuccess: (data) => {
      setMessages(prev => [
        ...prev,
        { role: 'ai', content: data.response }
      ]);
    },
  });

  useEffect(() => {
    if (textareaRef.current) {
      // 7줄 기준 max-height 계산: line-height(1.5) * font-size(16px) * 7줄 + padding(1.5rem) + border(4px)
      const maxHeight = 1.5 * 16 * 7 + 24 + 4; // 196px (padding 감소)
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, maxHeight)}px`;
    }
  }, [question]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, mutation.isPending]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim() || !user?.userId) return;

    setMessages(prev => [...prev, { role: 'user', content: question }]);
    mutation.mutate(question);
    setQuestion('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleExampleClick = (example: string) => {
    setQuestion(example);
    textareaRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Shift + Enter: 줄바꿈
    // Enter만: 메시지 전송
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (question.trim() && !mutation.isPending && user?.userId) {
        handleSubmit(e as any);
      }
    }
  };

  return (
    <div className="ai-chat">
      <div className="ai-chat-header">
        <div className="header-title-row">
          <h1>AI 주식 분석가에게 물어보세요</h1>
          <button
            className="info-icon-btn"
            onClick={() => setShowInfoCard(true)}
            title="AI 분석 기준 보기"
            type="button"
          >
            i
          </button>
        </div>
      </div>

      {showInfoCard && (
        <div className="info-modal-overlay" onClick={() => setShowInfoCard(false)}>
          <div className="info-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="info-modal-header">
              <h2>AI 분석 기준</h2>
              <button
                className="info-modal-close"
                onClick={() => setShowInfoCard(false)}
                type="button"
              >
                ✕
              </button>
            </div>
            <div className="info-modal-body">
              <ul>
                <li>최근 뉴스 + 시장 데이터 기반</li>
                <li>투자 조언 아님 (정보 제공 목적)</li>
                <li>감정/영향 점수 분석 포함</li>
              </ul>
            </div>
          </div>
        </div>
      )}
      
      <div className="chat-container">
        <div className="messages">
          {messages.length === 0 && (
            <div className="welcome">
              <div className="welcome-title">이런 질문을 할 수 있어요</div>
              <div className="example-questions">
                {exampleQuestions.map((example, idx) => (
                  <button
                    key={idx}
                    className="example-question-btn"
                    onClick={() => handleExampleClick(example)}
                    type="button"
                  >
                    {example}
                  </button>
                ))}
              </div>
            </div>
          )}
          {messages.map((msg, idx) => (
            <div key={idx} className={`message ${msg.role}`}>
              <div 
                className="message-content"
                dangerouslySetInnerHTML={{ __html: parseMarkdown(msg.content) }}
              />
            </div>
          ))}
          {mutation.isPending && (
            <div className="message ai">
              <div className="message-content loading-message">
                <span className="loading-spinner"></span>
                <span>분석 중...</span>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>
        
        <form onSubmit={handleSubmit} className="chat-input">
          <div className="textarea-wrapper">
            <div className="textarea-scroll">
              <textarea
                ref={textareaRef}
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="궁금한 종목이나 시장 이슈를 입력하세요"
                disabled={mutation.isPending}
                rows={1}
              />
            </div>
            <button 
              type="submit" 
              disabled={mutation.isPending || !question.trim()}
              className={`send-btn ${question.trim() ? 'active' : ''}`}
              title="전송"
            >
              <span className="send-icon">↑</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AIChat;

