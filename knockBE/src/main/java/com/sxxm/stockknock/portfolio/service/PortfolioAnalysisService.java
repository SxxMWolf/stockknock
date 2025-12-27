/**
 * 포트폴리오 AI 분석 서비스. 캐싱 및 재분석 로직 관리.
 */
package com.sxxm.stockknock.portfolio.service;

import com.sxxm.stockknock.ai.dto.AIResponseResult;
import com.sxxm.stockknock.ai.service.GPTClientService;
import com.sxxm.stockknock.portfolio.dto.PortfolioAnalysisDto;
import com.sxxm.stockknock.portfolio.entity.PortfolioAnalysis;
import com.sxxm.stockknock.portfolio.entity.PortfolioItem;
import com.sxxm.stockknock.portfolio.repository.PortfolioAnalysisRepository;
import com.sxxm.stockknock.portfolio.repository.PortfolioItemRepository;
import com.sxxm.stockknock.portfolio.util.PortfolioHashUtil;
import com.sxxm.stockknock.stock.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PortfolioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAnalysisService.class);

    @Autowired
    private PortfolioAnalysisRepository portfolioAnalysisRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private GPTClientService gptClientService;

    /**
     * 포트폴리오 분석 조회 (캐싱 로직 포함)
     * 1. 현재 포트폴리오 → hash 계산
     * 2. DB에서 기존 분석 조회
     *    - hash 동일 + status SUCCESS → DB 결과 반환
     *    - hash 다름 → AI 재분석
     *    - 데이터 없음 → AI 최초 분석
     */
    @Transactional
    public PortfolioAnalysisDto getPortfolioAnalysis(Long userId) {
        // 1. 현재 포트폴리오 조회
        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioUserId(userId);
        
        // 2. 포트폴리오 해시 계산
        String currentHash = PortfolioHashUtil.generateHash(items);
        
        // 3. 기존 분석 조회
        Optional<PortfolioAnalysis> existingAnalysis = portfolioAnalysisRepository.findByUserId(userId);
        
        // 4. 캐시 확인
        if (existingAnalysis.isPresent()) {
            PortfolioAnalysis analysis = existingAnalysis.get();
            
            // 해시가 동일하고 성공 상태면 캐시 사용
            if (currentHash.equals(analysis.getPortfolioHash()) && 
                analysis.getStatus() == PortfolioAnalysis.AnalysisStatus.SUCCESS) {
                log.info("[포트폴리오 분석] 캐시 사용 (userId={})", userId);
                return buildDtoFromCache(analysis, items);
            } else {
                log.info("[포트폴리오 분석] 포트폴리오 변경 감지 → AI 재분석 (userId={})", userId);
            }
        } else {
            log.info("[포트폴리오 분석] 최초 분석 (userId={})", userId);
        }
        
        // 5. AI 재분석 수행
        return performAnalysis(userId, items, currentHash, existingAnalysis);
    }

    /**
     * 포트폴리오 분석 캐시 삭제 (강제 재분석용)
     */
    @Transactional
    public void clearCache(Long userId) {
        portfolioAnalysisRepository.findByUserId(userId).ifPresent(analysis -> {
            portfolioAnalysisRepository.delete(analysis);
            log.info("[포트폴리오 분석] 캐시 삭제 완료 (userId={})", userId);
        });
    }

    /**
     * AI 분석 수행 및 DB 저장
     */
    private PortfolioAnalysisDto performAnalysis(Long userId, List<PortfolioItem> items, 
                                                  String currentHash, 
                                                  Optional<PortfolioAnalysis> existingAnalysis) {
        // 가격 정보 조회
        List<String> symbols = items.stream()
                .map(item -> item.getStock() != null ? item.getStock().getSymbol() : null)
                .filter(s -> s != null)
                .distinct()
                .collect(Collectors.toList());
        Map<String, BigDecimal> priceMap = stockService.getCurrentPricesBatch(symbols);

        // 포트폴리오 요약 생성
        StringBuilder summary = new StringBuilder();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            BigDecimal currentPrice = priceMap.getOrDefault(item.getStock().getSymbol(), BigDecimal.ZERO);
            
            BigDecimal itemValue = currentPrice.multiply(item.getQuantity());
            BigDecimal itemCost = item.getAvgBuyPrice().multiply(item.getQuantity());
            BigDecimal itemProfitLoss = currentPrice.subtract(item.getAvgBuyPrice())
                    .multiply(item.getQuantity());
            
            totalValue = totalValue.add(itemValue);
            totalCost = totalCost.add(itemCost);
            totalProfitLoss = totalProfitLoss.add(itemProfitLoss);

            summary.append(String.format(
                    "- %s (%s): 보유량 %s주, 평균가 %s원, 현재가 %s원, 손익 %s원\n",
                    item.getStock().getName(),
                    item.getStock().getSymbol(),
                    item.getQuantity(),
                    item.getAvgBuyPrice(),
                    currentPrice,
                    itemProfitLoss
            ));
        }

        summary.append(String.format("\n총 평가액: %s원\n", formatMoney(totalValue)));
        summary.append(String.format("총 매입금액: %s원\n", formatMoney(totalCost)));
        summary.append(String.format("총 손익: %s원\n", formatMoney(totalProfitLoss)));

        BigDecimal totalProfitLossRate = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossRate = totalProfitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // AI 분석 수행
        String aiAnalysis = generateBasicAnalysis(items, totalValue, totalCost, totalProfitLoss, priceMap);
        
        try {
            if (gptClientService != null) {
                String userPrompt = buildPortfolioDataPrompt(summary.toString(), items, 
                                                             totalValue, totalCost, totalProfitLoss, priceMap);
                String systemPrompt = getPortfolioSystemPrompt();
                
                com.sxxm.stockknock.ai.dto.AIRequestOptions options = 
                    com.sxxm.stockknock.ai.dto.AIRequestOptions.builder()
                        .systemPrompt(systemPrompt)
                        .temperature(0.7)
                        .maxTokens(1000)
                        .timeoutSeconds(90)
                        .build();
                
                AIResponseResult result = gptClientService.generateResponseAsync(userPrompt, options)
                        .block(java.time.Duration.ofSeconds(90));
                
                if (result != null && result.isSuccess() && result.getContent() != null && 
                    !result.getContent().isEmpty() &&
                    !result.getContent().contains("오류가 발생했습니다") &&
                    !result.getContent().contains("AI 분석 중 오류")) {
                    aiAnalysis = result.getContent();
                } else {
                    String errorMsg = result != null ? result.getErrorMessage() : "알 수 없는 오류";
                    log.warn("[포트폴리오 분석] GPT API 실패: {} (userId={})", errorMsg, userId);
                }
            }
        } catch (Exception e) {
            log.error("[포트폴리오 분석] GPT API 예외 (userId={}): {}", userId, e.getMessage(), e);
        }

        // DB 저장 (UPDATE 또는 INSERT)
        PortfolioAnalysis analysis = existingAnalysis.orElse(
                PortfolioAnalysis.builder()
                        .userId(userId)
                        .build()
        );
        
        analysis.setPortfolioHash(currentHash);
        analysis.setAnalysisContent(aiAnalysis);
        analysis.setStatus(PortfolioAnalysis.AnalysisStatus.SUCCESS);
        analysis.setUpdatedAt(LocalDateTime.now());
        
        portfolioAnalysisRepository.save(analysis);
        log.info("[포트폴리오 분석] 분석 결과 저장 완료 (userId={})", userId);

        return PortfolioAnalysisDto.builder()
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossRate(totalProfitLossRate)
                .analysis(aiAnalysis)
                .investmentStyle("BALANCED")
                .build();
    }

    /**
     * 캐시에서 DTO 생성
     */
    private PortfolioAnalysisDto buildDtoFromCache(PortfolioAnalysis analysis, List<PortfolioItem> items) {
        // 캐시된 분석 내용 사용, 하지만 현재 가격 정보는 다시 계산
        List<String> symbols = items.stream()
                .map(item -> item.getStock() != null ? item.getStock().getSymbol() : null)
                .filter(s -> s != null)
                .distinct()
                .collect(Collectors.toList());
        Map<String, BigDecimal> priceMap = stockService.getCurrentPricesBatch(symbols);

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            BigDecimal currentPrice = priceMap.getOrDefault(item.getStock().getSymbol(), BigDecimal.ZERO);
            BigDecimal itemValue = currentPrice.multiply(item.getQuantity());
            BigDecimal itemCost = item.getAvgBuyPrice().multiply(item.getQuantity());
            BigDecimal itemProfitLoss = currentPrice.subtract(item.getAvgBuyPrice())
                    .multiply(item.getQuantity());
            
            totalValue = totalValue.add(itemValue);
            totalCost = totalCost.add(itemCost);
            totalProfitLoss = totalProfitLoss.add(itemProfitLoss);
        }

        BigDecimal totalProfitLossRate = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossRate = totalProfitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return PortfolioAnalysisDto.builder()
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossRate(totalProfitLossRate)
                .analysis(analysis.getAnalysisContent())
                .investmentStyle("BALANCED")
                .build();
    }

    /**
     * 기본 포트폴리오 분석 (GPT 실패 시 폴백)
     */
    private String generateBasicAnalysis(List<PortfolioItem> items, BigDecimal totalValue, 
                                         BigDecimal totalCost, BigDecimal totalProfitLoss,
                                         Map<String, BigDecimal> priceMap) {
        BigDecimal totalProfitLossRate = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossRate = totalProfitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        StringBuilder analysis = new StringBuilder();
        analysis.append("■ 포트폴리오 요약\n");
        analysis.append(String.format("총 평가액: %s원, 총 매입금액: %s원, 총 손익: %s원, 수익률: %s%%\n",
                formatMoney(totalValue), formatMoney(totalCost), formatMoney(totalProfitLoss), formatRate(totalProfitLossRate)));
        
        for (PortfolioItem item : items) {
            BigDecimal currentPrice = priceMap.getOrDefault(item.getStock().getSymbol(), BigDecimal.ZERO);
            BigDecimal itemProfitLoss = currentPrice.subtract(item.getAvgBuyPrice())
                    .multiply(item.getQuantity());
            BigDecimal itemProfitRate = BigDecimal.ZERO;
            if (item.getAvgBuyPrice().compareTo(BigDecimal.ZERO) > 0) {
                itemProfitRate = currentPrice.subtract(item.getAvgBuyPrice())
                        .divide(item.getAvgBuyPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            analysis.append(String.format("• %s (%s): %s주, 평균가 %s원, 현재가 %s원, 손익 %s원 (%s%%)",
                item.getStock().getName(),
                item.getStock().getSymbol(),
                item.getQuantity().setScale(0, RoundingMode.HALF_UP).toPlainString(),
                formatMoney(item.getAvgBuyPrice()),
                formatMoney(currentPrice),
                formatMoney(itemProfitLoss),
                formatRate(itemProfitRate)));
            analysis.append("\n");
        }
        
        if (totalProfitLoss.compareTo(BigDecimal.ZERO) > 0) {
            analysis.append("■ 조언: 수익 실현 시점 고려");
        } else {
            analysis.append("■ 조언: 장기 투자 관점에서 종목 재검토");
        }
        
        return analysis.toString();
    }

    /**
     * 포트폴리오 분석 전용 System Prompt
     */
    private String getPortfolioSystemPrompt() {
        return """
            너는 개인 투자자를 위한 AI 포트폴리오 분석가다.
            
            중요 규칙:
            1. 수익률, 손익, 비중 수치는 이미 계산된 값을 그대로 해석만 한다.
               (새로운 계산을 시도하지 않는다)
            2. 특정 종목의 매수·매도·손절을 직접 지시하지 않는다.
            3. "추천", "확정", "반드시" 같은 표현을 사용하지 않는다.
            4. 모든 행동 제안은 "고려해볼 수 있다", "검토할 수 있다" 형태로 작성한다.
            5. 분석은 객관적이고 중립적인 리포트 톤을 유지한다.
            
            출력 형식:
            - 포트폴리오 해석 (1~2문장)
            - 핵심 리스크 또는 구조적 특징 (1~2문장)
            - 투자자가 고려해볼 선택지 (불릿 2~3개)
            
            주의:
            금융 조언처럼 보일 수 있는 단정적 표현을 피하고,
            시장 흐름과 구조 중심으로 설명한다.
            """;
    }

    /**
     * 포트폴리오 데이터 프롬프트 생성 (사용자 프롬프트)
     */
    private String buildPortfolioDataPrompt(String summary, List<PortfolioItem> items,
                                            BigDecimal totalValue, BigDecimal totalCost, 
                                            BigDecimal totalProfitLoss, Map<String, BigDecimal> priceMap) {
        BigDecimal totalProfitLossRate = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossRate = totalProfitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("아래 포트폴리오 데이터를 기반으로 분석해줘:\n\n");
        prompt.append("=== 포트폴리오 데이터 ===\n");
        prompt.append(summary);
        prompt.append(String.format("\n총 평가액: %s원\n", formatMoney(totalValue)));
        prompt.append(String.format("총 매입금액: %s원\n", formatMoney(totalCost)));
        prompt.append(String.format("총 손익: %s원\n", formatMoney(totalProfitLoss)));
        prompt.append(String.format("수익률: %s%%\n\n", formatRate(totalProfitLossRate)));
        
        prompt.append("=== 종목별 상세 정보 ===\n");
        for (PortfolioItem item : items) {
            BigDecimal currentPrice = priceMap.getOrDefault(item.getStock().getSymbol(), BigDecimal.ZERO);
            BigDecimal itemProfitLoss = currentPrice.subtract(item.getAvgBuyPrice())
                    .multiply(item.getQuantity());
            BigDecimal itemProfitRate = BigDecimal.ZERO;
            if (item.getAvgBuyPrice().compareTo(BigDecimal.ZERO) > 0) {
                itemProfitRate = currentPrice.subtract(item.getAvgBuyPrice())
                        .divide(item.getAvgBuyPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            prompt.append(String.format("- %s (%s): 보유량 %s주, 평균가 %s원, 현재가 %s원, 손익 %s원 (%s%%)\n",
                item.getStock().getName(),
                item.getStock().getSymbol(),
                item.getQuantity().setScale(0, RoundingMode.HALF_UP).toPlainString(),
                formatMoney(item.getAvgBuyPrice()),
                formatMoney(currentPrice),
                formatMoney(itemProfitLoss),
                formatRate(itemProfitRate)));
        }
        
        return prompt.toString();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0";
        return String.format("%,.0f", value);
    }

    private String formatRate(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format("%.2f", value);
    }
}

