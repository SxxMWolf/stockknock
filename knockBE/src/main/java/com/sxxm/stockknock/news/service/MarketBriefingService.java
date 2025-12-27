/**
 * 오늘의 시장 브리핑 생성 및 조회. 하루 1회 GPT로 생성하여 DB에 저장.
 */
package com.sxxm.stockknock.news.service;

import com.sxxm.stockknock.ai.dto.AIResponseResult;
import com.sxxm.stockknock.ai.service.GPTClientService;
import com.sxxm.stockknock.news.entity.BriefingStatus;
import com.sxxm.stockknock.news.entity.MarketBriefing;
import com.sxxm.stockknock.news.repository.MarketBriefingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MarketBriefingService {

    private static final Logger log = LoggerFactory.getLogger(MarketBriefingService.class);
    private static final Long GLOBAL_USER_ID = 0L;

    @Autowired
    private MarketBriefingRepository marketBriefingRepository;

    @Autowired
    private GPTClientService gptClientService;

    /**
     * 오늘의 전역 시장 브리핑 생성 (스케줄러 전용)
     */
    public boolean generateTodayGlobalBriefing() {
        LocalDate today = LocalDate.now();

        Optional<MarketBriefing> existing =
                marketBriefingRepository.findByUserIdAndDate(GLOBAL_USER_ID, today);

        if (existing.isPresent() && existing.get().getStatus() == BriefingStatus.SUCCESS) {
            log.info("[시장 브리핑] 이미 생성됨 (date: {})", today);
            return true;
        }

        log.info("[시장 브리핑] 전역 브리핑 생성 시작 (date: {})", today);

        String prompt = buildPrompt(today);

        try {
            AIResponseResult result = gptClientService.generateResponseAsync(prompt)
                    .block(java.time.Duration.ofSeconds(90));

            if (result != null && result.isSuccess()
                    && result.getContent() != null && !result.getContent().isBlank()) {

                MarketBriefing briefing = existing.orElse(
                        MarketBriefing.builder()
                                .userId(GLOBAL_USER_ID)
                                .date(today)
                                .build()
                );

                briefing.setContent(result.getContent());
                briefing.setStatus(BriefingStatus.SUCCESS);
                briefing.setCreatedAt(LocalDateTime.now());

                marketBriefingRepository.save(briefing);
                log.info("[시장 브리핑] 생성 성공 (date: {})", today);
                return true;
            }

            log.error("[시장 브리핑] GPT 실패: {}", 
                    result != null ? result.getErrorMessage() : "unknown");
            saveFailedBriefing(existing, today);
            return false;

        } catch (Exception e) {
            log.error("[시장 브리핑] GPT 예외", e);
            saveFailedBriefing(existing, today);
            return false;
        }
    }

    /**
     * 오늘의 전역 시장 브리핑 조회 (GPT 호출 없음)
     */
    public String getTodayGlobalBriefing() {
        LocalDate today = LocalDate.now();

        return marketBriefingRepository
                .findByUserIdAndDate(GLOBAL_USER_ID, today)
                .filter(b -> b.getStatus() == BriefingStatus.SUCCESS)
                .map(MarketBriefing::getContent)
                .orElse("오늘의 시장 브리핑을 준비 중입니다. 잠시 후 다시 확인해주세요.");
    }

    /**
     * 실패 상태 저장
     */
    private void saveFailedBriefing(Optional<MarketBriefing> existing, LocalDate date) {
        MarketBriefing briefing = existing.orElse(
                MarketBriefing.builder()
                        .userId(GLOBAL_USER_ID)
                        .date(date)
                        .build()
        );

        briefing.setContent(null); // 사용자에게 노출 X
        briefing.setStatus(BriefingStatus.FAILED);
        briefing.setCreatedAt(LocalDateTime.now());

        marketBriefingRepository.save(briefing);
        log.info("[시장 브리핑] 실패 상태 저장 (date: {})", date);
    }

    /**
     * GPT 프롬프트 생성
     */
    private String buildPrompt(LocalDate today) {
        return """
        너는 한국 주식 시장 아침 브리핑을 작성하는 금융 리포터야.

        오늘 날짜(%s) 기준으로
        한국 주식 시장 상황을 정확히 5줄로 요약해줘.

        작성 규칙:
        1. 각 줄은 한 문장만 작성
        2. 애매한 표현 금지 (예: ~보입니다, ~가능성)
        3. 종목 나열보다 시장 흐름 중심
        4. 투자자가 아침에 바로 읽을 수 있는 톤
        5. 마지막 줄은 오늘 시장을 한 단어로 요약

        반드시 정확히 5줄로만 작성하고
        머리말이나 설명 문장은 쓰지 마.
        """.formatted(today);
    }
}
