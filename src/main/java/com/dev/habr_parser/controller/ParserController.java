package com.dev.habr_parser.controller;

import com.dev.habr_parser.service.ParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/parser")
@RequiredArgsConstructor
public class ParserController {

    private final ParserService parserService;

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String url,
                                           @RequestParam(defaultValue = "0") int commentLimit) {

        String markdown = parserService.parseToMarkdown(url, commentLimit);
        byte[] content = markdown.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"article.md\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(content);
    }
}