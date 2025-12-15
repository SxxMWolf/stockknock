-- ============================================
-- StockKnock Database Schema
-- PostgreSQL
-- ============================================

-- 1. users (사용자)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 참고: UNIQUE 제약조건이 자동으로 인덱스를 생성하므로 별도 인덱스 생성 불필요
-- CREATE INDEX idx_users_email ON users(email); -- UNIQUE 제약조건이 이미 인덱스 생성

-- 2. stocks (종목 기본 정보)
CREATE TABLE IF NOT EXISTS stocks (
    symbol VARCHAR(20) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    exchange VARCHAR(50),
    country VARCHAR(50),
    industry VARCHAR(100),
    currency VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stocks_exchange ON stocks(exchange);
CREATE INDEX idx_stocks_country ON stocks(country);
CREATE INDEX idx_stocks_industry ON stocks(industry);

-- 3. stock_price_history (종목 시세 히스토리)
CREATE TABLE IF NOT EXISTS stock_price_history (
    id BIGSERIAL PRIMARY KEY,
    stock_symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol) ON DELETE CASCADE,
    price NUMERIC(18,4) NOT NULL,
    open NUMERIC(18,4),
    high NUMERIC(18,4),
    low NUMERIC(18,4),
    volume BIGINT,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(stock_symbol, timestamp)
);

CREATE INDEX idx_price_history_stock_time ON stock_price_history(stock_symbol, timestamp DESC);
CREATE INDEX idx_price_history_timestamp ON stock_price_history(timestamp DESC);

-- 4. portfolio (사용자 포트폴리오)
CREATE TABLE IF NOT EXISTS portfolio (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) DEFAULT 'Default Portfolio',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_portfolio_user ON portfolio(user_id);

-- 5. portfolio_item (포트폴리오 개별 보유 종목)
CREATE TABLE IF NOT EXISTS portfolio_item (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolio(id) ON DELETE CASCADE,
    stock_symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol) ON DELETE CASCADE,
    quantity NUMERIC(18,4) NOT NULL DEFAULT 0,
    avg_buy_price NUMERIC(18,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_portfolio_stock ON portfolio_item(portfolio_id, stock_symbol);
CREATE INDEX idx_portfolio_item_stock ON portfolio_item(stock_symbol);

-- 6. watchlist (관심 종목)
CREATE TABLE IF NOT EXISTS watchlist (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stock_symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, stock_symbol)
);

CREATE INDEX idx_watchlist_user ON watchlist(user_id);
CREATE INDEX idx_watchlist_stock ON watchlist(stock_symbol);

-- 7. price_alert (가격 알림 설정)
CREATE TABLE IF NOT EXISTS price_alert (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stock_symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol) ON DELETE CASCADE,
    alert_type VARCHAR(20) NOT NULL, -- 'TARGET', 'STOP_LOSS', 'PERCENT'
    target_price NUMERIC(18,4),
    percent_change NUMERIC(5,2),
    triggered BOOLEAN DEFAULT FALSE,
    triggered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_price_alert_user ON price_alert(user_id);
CREATE INDEX idx_price_alert_stock ON price_alert(stock_symbol);
CREATE INDEX idx_price_alert_active ON price_alert(user_id, triggered) WHERE triggered = FALSE;

-- 8. news (수집된 뉴스 원문)
CREATE TABLE IF NOT EXISTS news (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT,
    url TEXT,
    source VARCHAR(255),
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_news_published ON news(published_at DESC);
CREATE INDEX idx_news_created ON news(created_at DESC);
CREATE INDEX idx_news_source ON news(source);

-- 9. news_analysis (AI 분석 데이터)
CREATE TABLE IF NOT EXISTS news_analysis (
    news_id BIGINT PRIMARY KEY REFERENCES news(id) ON DELETE CASCADE,
    summary TEXT,
    sentiment VARCHAR(20), -- 'positive', 'negative', 'neutral'
    impact_score INT CHECK (impact_score >= 1 AND impact_score <= 10),
    ai_comment TEXT,
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_news_analysis_sentiment ON news_analysis(sentiment);
CREATE INDEX idx_news_analysis_impact ON news_analysis(impact_score DESC);

-- 10. news_stock_relation (뉴스 ↔ 종목 연관 N:M)
CREATE TABLE IF NOT EXISTS news_stock_relation (
    news_id BIGINT NOT NULL REFERENCES news(id) ON DELETE CASCADE,
    stock_symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (news_id, stock_symbol)
);

CREATE INDEX idx_news_stock_news ON news_stock_relation(news_id);
CREATE INDEX idx_news_stock_stock ON news_stock_relation(stock_symbol);

-- 11. ai_conversation (AI 대화 기록)
CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL, -- 'user', 'assistant'
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conversation_user_time ON ai_conversation(user_id, created_at DESC);

-- 12. email_verification (이메일 인증 코드)
CREATE TABLE IF NOT EXISTS email_verification (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verification_email ON email_verification(email);
CREATE INDEX idx_email_verification_code ON email_verification(verification_code);

