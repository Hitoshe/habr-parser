package com.dev.habr_parser.service;

import com.dev.habr_parser.entity.Article;
import com.dev.habr_parser.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParserService {

    private final ArticleRepository articleRepository;

    public Article parseAndSave(String url) {
        try {
            // 1. Подключаемся к Хабру
            // .get() скачивает весь HTML код страницы
            Document doc = Jsoup.connect(url).get();

            // 2. Извлекаем заголовок
            // На Хабре заголовок лежит в теге h1 :з
            String title = doc.select("h1").text();

            // 3. Создаем объект статьи
            Article article = new Article();
            article.setTitle(title);
            article.setUrl(url);
            article.setContent("Тут скоро будет Markdown текст..."); // Пока заглушка
            article.setParsedAt(LocalDateTime.now());

            // 4. Сохраняем в базу данных через репозиторий
            return articleRepository.save(article);

        } catch (IOException e) {
            throw new RuntimeException("Не удалось спарсить статью: " + e.getMessage());
        }
    }
}