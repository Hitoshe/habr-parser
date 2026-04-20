package com.dev.habr_parser.service;

import com.dev.habr_parser.entity.Article;
import com.dev.habr_parser.repository.ArticleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ParserService {

    private final ArticleRepository articleRepository;
    private final ObjectMapper objectMapper;

    public Article parseAndSave(String url, int commentLimit) {
        try {
            // 1. Загружаем основную страницу для текста статьи
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get();

            String title = doc.select("h1").text();

            // 2. Парсим контент методом глубокого обхода узлов
            Element body = doc.selectFirst(".article-formatted-body, .tm-article-body, #post-content-body");
            String markdown = "";
            if (body != null) {
                markdown = nodesToMarkdown(body);
            }

            // 3. ПАРСИНГ КОММЕНТАРИЕВ ЧЕРЕЗ API ХАБРА
            String commentsPart = "";
            if (commentLimit > 0) {
                commentsPart = fetchCommentsViaApi(url, commentLimit);
            }

            // 4. Логика сохранения или обновления в БД
            Optional<Article> existing = articleRepository.findByUrl(url);
            Article article = existing.orElse(new Article());

            String frontmatter = "---\n" +
                    "title: \"" + title.replace("\"", "'") + "\"\n" +
                    "url: " + url + "\n" +
                    "parsed: " + LocalDateTime.now() + "\n" +
                    "tags: [habr, auto-parsed]\n" +
                    "---\n\n";

            article.setTitle(title);
            article.setUrl(url);
            article.setContent(frontmatter + "# " + title + "\n\n" + markdown + commentsPart);
            article.setParsedAt(LocalDateTime.now());

            return articleRepository.save(article);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при работе с Хабром: " + e.getMessage());
        }
    }

    /**
     * Получает комментарии напрямую из API Хабра, обходя JavaScript
     */
    private String fetchCommentsViaApi(String url, int limit) {
        try {
            String articleId = extractId(url);
            String apiUrl = "https://habr.com/kek/v2/articles/" + articleId + "/comments/?fl=ru%2Cen&hl=ru";

            // Тянем чистый JSON
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            // Парсим JSON
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode commentsMap = root.get("comments"); // На Хабре это объект, где ключи - ID комментов

            if (commentsMap == null || commentsMap.isEmpty()) return "";

            StringBuilder sb = new StringBuilder("\n\n---\n## Комментарии\n\n");
            int count = 0;

            // Перебираем комментарии из мапы
            for (JsonNode commentNode : commentsMap) {
                if (count >= limit) break;

                String author = commentNode.get("author").get("alias").asText();
                String htmlMessage = commentNode.get("message").asText();

                // Текст комментария тоже может содержать HTML, чистим его Jsoup'ом
                String cleanText = Jsoup.parse(htmlMessage).text();

                sb.append("> **").append(author).append("**: ").append(cleanText).append("\n\n");
                count++;
            }
            return sb.toString();

        } catch (Exception e) {
            return "\n\n---\n*Не удалось загрузить комментарии (API недоступно)*";
        }
    }

    /**
     * Вырезает ID статьи из ссылки (например 359348)
     */
    private String extractId(String url) {
        Pattern pattern = Pattern.compile("articles/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Если это ссылка типа /news/
        pattern = Pattern.compile("news/(\\d+)");
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("ID статьи не найден в URL");
    }

    /**
     * Обходит все узлы (текст, теги, переносы) по порядку
     */
    private String nodesToMarkdown(Node parent) {
        StringBuilder sb = new StringBuilder();

        for (Node node : parent.childNodes()) {
            if (node instanceof TextNode) {
                sb.append(((TextNode) node).text());
            } else if (node instanceof Element) {
                Element el = (Element) node;
                String tag = el.tagName();

                switch (tag) {
                    case "h1": case "h2": case "h3": case "h4":
                        sb.append("\n\n").append("#".repeat(tag.charAt(1) - '0'))
                                .append(" ").append(el.text()).append("\n\n");
                        break;
                    case "b": case "strong":
                        sb.append("**").append(nodesToMarkdown(el)).append("**");
                        break;
                    case "i": case "em":
                        sb.append("*").append(nodesToMarkdown(el)).append("*");
                        break;
                    case "a":
                        sb.append("[").append(el.text()).append("](").append(el.attr("abs:href")).append(")");
                        break;
                    case "br":
                        sb.append("\n");
                        break;
                    case "p": case "div":
                        sb.append("\n\n").append(nodesToMarkdown(el)).append("\n\n");
                        break;
                    case "img":
                        String src = el.attr("abs:src");
                        if (src.isEmpty()) src = el.attr("abs:data-src");
                        if (!src.isEmpty()) sb.append("\n\n![](").append(src).append(")\n\n");
                        break;
                    case "ul":
                        sb.append("\n");
                        for (Element li : el.select("li")) sb.append("- ").append(nodesToMarkdown(li)).append("\n");
                        sb.append("\n");
                        break;
                    case "ol":
                        sb.append("\n");
                        int i = 1;
                        for (Element li : el.select("li")) sb.append(i++).append(". ").append(nodesToMarkdown(li)).append("\n");
                        sb.append("\n");
                        break;
                    case "code":
                        sb.append("`").append(el.text()).append("`");
                        break;
                    case "pre":
                        sb.append("\n\n```\n").append(el.text()).append("\n```\n\n");
                        break;
                    default:
                        sb.append(nodesToMarkdown(el));
                        break;
                }
            }
        }
        return sb.toString().replaceAll("(\n\n)+", "\n\n");
    }
}