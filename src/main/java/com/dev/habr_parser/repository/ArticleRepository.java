package com.dev.habr_parser.repository;

import com.dev.habr_parser.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // <Тип сущности, Тип её ID>

    // SELECT * FROM articles WHERE url = ?
    Optional<Article> findByUrl(String url);
}