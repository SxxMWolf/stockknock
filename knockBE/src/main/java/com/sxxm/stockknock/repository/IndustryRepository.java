package com.sxxm.stockknock.repository;

import com.sxxm.stockknock.entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, Long> {
}

