package com.sxxm.stockknock.watchlist.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistId implements Serializable {
    private Long userId;
    private String stockSymbol;
}

