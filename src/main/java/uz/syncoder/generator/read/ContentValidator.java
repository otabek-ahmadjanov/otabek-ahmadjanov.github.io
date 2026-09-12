package uz.syncoder.generator.read;

import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.model.LocalizedText;
import uz.syncoder.generator.model.Page;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentValidator {

    private static final Pattern IMAGE_LINK = Pattern.compile("]\\(/articles/[^)/]+/images/([^)\\s]+)\\)");
    private static final String IMAGE_PREFIX = "images/";

    public void validate(List<Article> articles, List<Page> pages, Problems problems) {
        checkUniqueSlugs(articles, Article::slug, Article::key, problems);
        checkUniqueSlugs(pages, Page::slug, Page::key, problems);

        for (Article article : articles) {
            if (article.published() && article.publishedAt() == null) {
                problems.add(article.key(), "published article has no 'publishedAt'");
            }
            if (!article.hasTranslation(Language.RU) && !article.hasTranslation(Language.EN)) {
                problems.add(article.key(), "has neither content.ru.md nor content.en.md");
            }
            checkCover(article, problems);
            checkInlineImages(article, problems);
        }
        for (Page page : pages) {
            if (!page.hasTranslation(Language.RU) && !page.hasTranslation(Language.EN)) {
                problems.add(page.key(), "has neither content.ru.md nor content.en.md");
            }
        }
    }

    private <T> void checkUniqueSlugs(
            List<T> items,
            java.util.function.Function<T, String> slug,
            java.util.function.Function<T, String> key,
            Problems problems) {
        Map<String, String> owners = new HashMap<>();
        for (T item : items) {
            String previous = owners.putIfAbsent(slug.apply(item), key.apply(item));
            if (previous != null) {
                problems.add(key.apply(item), "slug '" + slug.apply(item) + "' is already used by " + previous);
            }
        }
    }

    private void checkCover(Article article, Problems problems) {
        for (String cover : values(article.cover())) {
            if (cover.startsWith(IMAGE_PREFIX) && !exists(article.directory(), cover.substring(IMAGE_PREFIX.length()))) {
                problems.add(article.key(), "cover file '" + cover + "' does not exist");
            }
        }
    }

    private void checkInlineImages(Article article, Problems problems) {
        Set<String> missing = new LinkedHashSet<>();
        for (Language language : Language.values()) {
            String body = article.body().get(language);
            if (body == null) {
                continue;
            }
            Matcher matcher = IMAGE_LINK.matcher(body);
            while (matcher.find()) {
                String name = matcher.group(1);
                if (!exists(article.directory(), name)) {
                    missing.add(name);
                }
            }
        }
        missing.forEach(name -> problems.add(article.key(), "image 'images/" + name + "' is referenced but missing"));
    }

    private boolean exists(Path directory, String imageName) {
        return Files.isRegularFile(directory.resolve(IMAGE_PREFIX).resolve(imageName));
    }

    private Set<String> values(LocalizedText text) {
        Set<String> values = new HashSet<>();
        if (text != null) {
            for (Language language : Language.values()) {
                if (text.has(language)) {
                    values.add(text.get(language));
                }
            }
        }
        return values;
    }
}
