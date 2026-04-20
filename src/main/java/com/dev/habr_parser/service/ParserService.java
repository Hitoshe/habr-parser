package com.dev.habr_parser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ParserService {

    private final ObjectMapper objectMapper;

    /**
     * Основной метод: превращает URL в готовую строку Markdown
     */
    public String parseToMarkdown(String url, int commentLimit) {
        try {
            if (!url.contains("habr.com")) {
                throw new IllegalArgumentException("Поддерживаются только ссылки на Habr.com");
            }

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            String title = doc.select("h1").text();

            Element body = doc.selectFirst(".article-formatted-body, .tm-article-body, #post-content-body");
            String markdownContent = (body != null) ? nodesToMarkdown(body) : "*Контент не найден*";

            String frontmatter = "----- \n" +
                    "title: \"" + title.replace("\"", "'") + "\"\n" +
                    "url: " + url + "\n" +
                    "parsed: " + LocalDateTime.now() + "\n" +
                    "tags: [habr]\n" +
                    "--- \n\n";

            StringBuilder result = new StringBuilder();
            result.append(frontmatter).append("# ").append(title).append("\n\n");

            Element leadImg = doc.selectFirst(".tm-article-presenter__lead-image img, .tm-article-snippet__lead-image img");
            if (leadImg != null) {
                String src = leadImg.attr("abs:src").isEmpty() ? leadImg.attr("abs:data-src") : leadImg.attr("abs:src");
                if (!src.isEmpty()) result.append("![](").append(src).append(")\n\n");
            }

            result.append(markdownContent);

            if (commentLimit > 0) {
                result.append(fetchCommentsViaApi(url, commentLimit));
            }

            return result.toString();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении страницы: " + e.getMessage());
        }
    }

    /**
     * Рекурсивный обход всех узлов (текст, теги) для сохранения структуры
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
                    case "p": case "div":
                        sb.append("\n\n").append(nodesToMarkdown(el)).append("\n\n");
                        break;
                    case "br":
                        sb.append("\n");
                        break;
                    case "h1": case "h2": case "h3": case "h4":
                        int level = tag.length() > 1 ? Character.getNumericValue(tag.charAt(1)) : 2;
                        sb.append("\n\n").append("#".repeat(level)).append(" ").append(el.text()).append("\n\n");
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
                    case "img":
                        String src = el.attr("abs:src").isEmpty() ? el.attr("abs:data-src") : el.attr("abs:src");
                        if (!src.isEmpty()) sb.append("\n\n![](").append(src).append(")\n\n");
                        break;
                    case "ul":
                        sb.append("\n");
                        for (Element li : el.select("li")) sb.append("- ").append(nodesToMarkdown(li)).append("\n");
                        break;
                    case "ol":
                        sb.append("\n");
                        int i = 1;
                        for (Element li : el.select("li")) sb.append(i++).append(". ").append(nodesToMarkdown(li)).append("\n");
                        break;
                    case "pre":
                        sb.append("\n\n```\n").append(el.text()).append("\n```\n\n");
                        break;
                    case "code":
                        sb.append("`").append(el.text()).append("`");
                        break;
                    default:
                        sb.append(nodesToMarkdown(el));
                        break;
                }
            }
        }
        return sb.toString().replaceAll("(\n\n)+", "\n\n");
    }

    /**
     * Запрос комментариев из официального API Хабра
     */
    private String fetchCommentsViaApi(String url, int limit) {
        try {
            String articleId = extractId(url);
            String apiUrl = "https://habr.com/kek/v2/articles/" + articleId + "/comments/?fl=ru%2Cen&hl=ru";

            String jsonResponse = Jsoup.connect(apiUrl).ignoreContentType(true).userAgent("Mozilla/5.0").execute().body();
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode commentsMap = root.get("comments");

            if (commentsMap == null || commentsMap.isEmpty()) return "";

            StringBuilder sb = new StringBuilder("\n\n---\n## Комментарии\n\n");
            int count = 0;
            for (JsonNode commentNode : commentsMap) {
                if (count >= limit) break;
                String author = commentNode.get("author").get("alias").asText();
                String cleanText = Jsoup.parse(commentNode.get("message").asText()).text();
                sb.append("> **").append(author).append("**: ").append(cleanText).append("\n\n");
                count++;
            }
            return sb.toString();
        } catch (Exception e) {
            return "\n\n---\n*Комментарии недоступны*";
        }
    }

    private String extractId(String url) {
        Matcher m = Pattern.compile("(articles|news)/(\\d+)").matcher(url);
        if (m.find()) return m.group(2);
        throw new RuntimeException("ID не найден");
    }
}