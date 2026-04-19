package com.dev.habr_parser.repository;

import com.dev.habr_parser.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // <Тип сущности, Тип её ID>
}