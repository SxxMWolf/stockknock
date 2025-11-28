package com.sxxm.stockknock.repository;

import com.sxxm.stockknock.entity.EarningsCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EarningsCalendarRepository extends JpaRepository<EarningsCalendar, Long> {
    List<EarningsCalendar> findByEarningsDateBetween(LocalDate start, LocalDate end);
    List<EarningsCalendar> findByStockId(Long stockId);
}

