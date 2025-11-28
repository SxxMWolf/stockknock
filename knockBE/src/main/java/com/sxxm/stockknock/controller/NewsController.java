package com.sxxm.stockknock.controller;

import com.sxxm.stockknock.dto.NewsAnalysisDto;
import com.sxxm.stockknock.dto.NewsDto;
import com.sxxm.stockknock.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "http://localhost:3000")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping("/recent")
    public ResponseEntity<List<NewsDto>> getRecentNews(@RequestParam(defaultValue = "7") int days) {
        List<NewsDto> news = newsService.getRecentNews(days);
        return ResponseEntity.ok(news);
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<NewsDto> getNewsById(@PathVariable Long newsId) {
        try {
            NewsDto news = newsService.getNewsById(newsId);
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{newsId}/analyze")
    public ResponseEntity<NewsAnalysisDto> analyzeNews(@PathVariable Long newsId) {
        try {
            NewsAnalysisDto analysis = newsService.analyzeNews(newsId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

