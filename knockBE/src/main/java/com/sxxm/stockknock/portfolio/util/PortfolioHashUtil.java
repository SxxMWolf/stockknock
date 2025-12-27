/**
 * 포트폴리오 해시 생성 유틸리티. 종목 구성 변경 감지용.
 */
package com.sxxm.stockknock.portfolio.util;

import com.sxxm.stockknock.portfolio.entity.PortfolioItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PortfolioHashUtil {

    /**
     * 포트폴리오 아이템 목록을 기준으로 해시 생성
     * 기준: stock_symbol, quantity, avg_buy_price
     * 정렬 후 문자열로 만들고 SHA-256 해시
     */
    public static String generateHash(List<PortfolioItem> items) {
        if (items == null || items.isEmpty()) {
            return hashString("EMPTY_PORTFOLIO");
        }

        // 종목 심볼 기준으로 정렬하여 일관된 해시 생성
        String portfolioString = items.stream()
                .filter(item -> item.getStock() != null)
                .sorted(Comparator.comparing(item -> item.getStock().getSymbol()))
                .map(item -> String.format("%s:%s:%s",
                        item.getStock().getSymbol(),
                        item.getQuantity().toPlainString(),
                        item.getAvgBuyPrice().toPlainString()))
                .collect(Collectors.joining("|"));

        return hashString(portfolioString);
    }

    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}

