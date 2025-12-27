package com.sxxm.stockknock.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddPortfolioRequest {
    private String stockSymbol;
    private String quantity;  // String으로 받아서 변환
    private String avgBuyPrice;  // String으로 받아서 변환
    
    public BigDecimal getQuantityAsBigDecimal() {
        return quantity != null ? new BigDecimal(quantity) : null;
    }
    
    public BigDecimal getAvgBuyPriceAsBigDecimal() {
        return avgBuyPrice != null ? new BigDecimal(avgBuyPrice) : null;
    }
}

