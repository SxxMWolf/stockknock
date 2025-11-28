package com.sxxm.stockknock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDto {
    private Long id;
    private String title;
    private String content;
    private String source;
    private String url;
    private LocalDateTime publishedAt;
    private List<String> relatedStockSymbols;
    private NewsAnalysisDto analysis;
}

