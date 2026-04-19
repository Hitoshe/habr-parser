package com.dev.habr_parser.controller;

import com.dev.habr_parser.entity.Article;
import com.dev.habr_parser.service.ParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parser")
@RequiredArgsConstructor
public class ParserController {

    private final ParserService parserService;

    // POST http://localhost:8080/api/parser/parse?url=ССЫЛКА
    @PostMapping("/parse")
    public Article parseArticle(@RequestParam String url) {
        return parserService.parseAndSave(url);
    }
}