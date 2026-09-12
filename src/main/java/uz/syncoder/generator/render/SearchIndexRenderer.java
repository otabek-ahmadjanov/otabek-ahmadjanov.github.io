package uz.syncoder.generator.render;

import uz.syncoder.generator.markdown.MarkdownRenderer;
import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.write.Json;
import uz.syncoder.generator.write.SiteWriter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchIndexRenderer {

    private final MarkdownRenderer markdown;
    private final SiteWriter writer;

    public SearchIndexRenderer(MarkdownRenderer markdown, SiteWriter writer) {
        this.markdown = markdown;
        this.writer = writer;
    }

    public static String indexUrl(Language language) {
        return "/search-index." + language.code() + ".json";
    }

    public void render(List<Article> articles, Language language) {
        List<String> entries = articles.stream()
                .filter(article -> article.hasTranslation(language))
                .map(article -> entry(article, language))
                .toList();
        writer.write(indexUrl(language), Json.array(entries));
    }

    private String entry(Article article, Language language) {
        String body = article.body().resolve(language);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", Json.quote(article.title().resolve(language)));
        fields.put("summary", Json.quote(summary(article, language)));
        fields.put("url", Json.quote(article.url(language)));
        fields.put("tags", Json.strings(article.tags()));
        fields.put("headings", Json.strings(markdown.headings(body)));
        return Json.object(fields);
    }

    private String summary(Article article, Language language) {
        return article.summary() == null ? "" : article.summary().resolve(language);
    }
}
