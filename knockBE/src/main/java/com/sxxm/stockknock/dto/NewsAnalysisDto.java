package com.sxxm.stockknock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsAnalysisDto {
    private String summary;
    private String impactAnalysis;
    private String sentiment;
    private Integer impactScore;
}

