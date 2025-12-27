/**
 * Spring Boot 애플리케이션 진입점. 스케줄러 활성화.
 */
package com.sxxm.stockknock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 스케줄러 활성화
public class StockknockApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockknockApplication.class, args);
	}

}
