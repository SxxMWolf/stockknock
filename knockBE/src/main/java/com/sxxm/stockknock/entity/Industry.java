package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "industries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Industry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // 산업명 (예: IT, 금융, 바이오)

    private String description;

    @OneToMany(mappedBy = "industry")
    private List<Stock> stocks;
}

