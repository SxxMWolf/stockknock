package com.sxxm.stockknock.service;

import com.sxxm.stockknock.ai.AIService;
import com.sxxm.stockknock.entity.News;
import com.sxxm.stockknock.entity.Stock;
import com.sxxm.stockknock.repository.NewsRepository;
import com.sxxm.stockknock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 뉴스와 종목을 연관시키는 서비스
 * AI를 활용하여 뉴스 내용에서 등장하는 종목을 자동으로 추출
 */
@Service
public class NewsStockAssociationService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private AIService aiService;

    /**
     * 뉴스에서 등장하는 종목을 AI로 추출하고 연관시킴
     */
    public void associateStocksWithNews(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("뉴스를 찾을 수 없습니다."));

        // 모든 종목 심볼 목록 가져오기
        List<Stock> allStocks = stockRepository.findAll();
        String stockSymbols = allStocks.stream()
                .map(Stock::getSymbol)
                .collect(Collectors.joining(", "));

        // AI에게 뉴스 내용에서 등장하는 종목 심볼 추출 요청
        String prompt = String.format(
                "다음 뉴스 기사 내용을 분석하여, 언급된 주식 종목의 심볼을 추출해주세요.\n\n" +
                "뉴스 제목: %s\n" +
                "뉴스 내용: %s\n\n" +
                "가능한 종목 심볼 목록: %s\n\n" +
                "뉴스에서 언급된 종목 심볼만 쉼표로 구분하여 나열해주세요. 예: AAPL, MSFT, 005930\n" +
                "언급된 종목이 없으면 'NONE'이라고 답변해주세요.",
                news.getTitle(), news.getContent(), stockSymbols
        );

        String aiResponse = aiService.answerQuestion(prompt, "");

        // AI 응답에서 종목 심볼 추출
        List<Stock> relatedStocks = extractStocksFromAIResponse(aiResponse, allStocks);

        // 뉴스에 종목 연관
        if (news.getRelatedStocks() == null) {
            news.setRelatedStocks(new ArrayList<>());
        }
        news.getRelatedStocks().clear();
        news.getRelatedStocks().addAll(relatedStocks);
        newsRepository.save(news);
    }

    /**
     * AI 응답에서 종목 심볼 추출
     */
    private List<Stock> extractStocksFromAIResponse(String aiResponse, List<Stock> allStocks) {
        List<Stock> foundStocks = new ArrayList<>();

        if (aiResponse == null || aiResponse.contains("NONE")) {
            return foundStocks;
        }

        // 쉼표로 구분된 심볼 추출
        String[] parts = aiResponse.split(",");
        for (String part : parts) {
            String symbol = part.trim().toUpperCase();
            
            // 종목 목록에서 찾기
            Stock stock = allStocks.stream()
                    .filter(s -> s.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst()
                    .orElse(null);

            if (stock != null && !foundStocks.contains(stock)) {
                foundStocks.add(stock);
            }
        }

        return foundStocks;
    }

    /**
     * 모든 뉴스에 대해 종목 연관 분석 수행
     */
    public void associateAllNewsWithStocks() {
        List<News> allNews = newsRepository.findAll();
        for (News news : allNews) {
            if (news.getRelatedStocks() == null || news.getRelatedStocks().isEmpty()) {
                try {
                    associateStocksWithNews(news.getId());
                    Thread.sleep(1000); // API 제한 방지
                } catch (Exception e) {
                    System.err.println("뉴스-종목 연관 분석 오류: " + news.getId() + " - " + e.getMessage());
                }
            }
        }
    }
}

