package com.dev.habr_parser.controller;

import com.dev.habr_parser.entity.Article;
import com.dev.habr_parser.repository.ArticleRepository;
import com.dev.habr_parser.service.ParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parser")
@RequiredArgsConstructor
public class ParserController {

    private final ParserService parserService;
    private final ArticleRepository articleRepository;

    @PostMapping("/parse")
    public Article parseArticle(@RequestParam String url,
                                @RequestParam(defaultValue = "0") int commentLimit) {
        return parserService.parseAndSave(url, commentLimit);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadArticle(@PathVariable Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));

        byte[] content = article.getContent().getBytes();
        String fileName = "habr_" + id + ".md";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(content);
    }
}