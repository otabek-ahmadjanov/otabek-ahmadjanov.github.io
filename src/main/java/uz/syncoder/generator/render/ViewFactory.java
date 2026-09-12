package uz.syncoder.generator.render;

import uz.syncoder.generator.markdown.MarkdownRenderer;
import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.model.LocalizedText;
import uz.syncoder.generator.model.Page;
import uz.syncoder.generator.model.Tags;
import uz.syncoder.generator.render.view.ArticleView;
import uz.syncoder.generator.render.view.PageView;
import uz.syncoder.generator.render.view.TagView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewFactory {

    private static final Map<Language, DateTimeFormatter> DATE_FORMATS = new EnumMap<>(Map.of(
            Language.RU, DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru")),
            Language.EN, DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));

    private final MarkdownRenderer markdown;

    public ViewFactory(MarkdownRenderer markdown) {
        this.markdown = markdown;
    }

    public ArticleView article(Article article, Language language) {
        String body = article.body().resolve(language);
        return new ArticleView(
                article.slug(),
                article.url(language),
                article.title().resolve(language),
                resolve(article.summary(), language),
                markdown.toHtml(body),
                coverUrl(article, language),
                formatDate(article.publishedAt(), language),
                markdown.readingMinutes(body),
                tagViews(article, language));
    }

    private List<TagView> tagViews(Article article, Language language) {
        return article.tags().stream()
                .map(tag -> new TagView(tag, Tags.url(language, tag), 0))
                .toList();
    }

    public PageView page(Page page, Language language) {
        return new PageView(
                page.slug(),
                page.url(language),
                page.title().resolve(language),
                resolve(page.summary(), language),
                markdown.toHtml(page.body().resolve(language)));
    }

    public String formatDate(LocalDate date, Language language) {
        return date == null ? null : DATE_FORMATS.get(language).format(date);
    }

    private String coverUrl(Article article, Language language) {
        String cover = resolve(article.cover(), language);
        if (cover == null) {
            return null;
        }
        return cover.startsWith("images/") ? article.assetBase() + cover.substring("images/".length()) : cover;
    }

    private String resolve(LocalizedText text, Language language) {
        return text == null ? null : text.resolve(language);
    }
}
