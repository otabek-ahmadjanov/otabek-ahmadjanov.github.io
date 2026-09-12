package uz.syncoder.generator.model;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public record Article(
        String key,
        String slug,
        LocalizedText title,
        LocalizedText summary,
        LocalizedText body,
        LocalizedText cover,
        List<String> tags,
        boolean published,
        LocalDate publishedAt,
        Path directory) {

    public boolean hasTranslation(Language language) {
        return body != null && body.has(language);
    }

    public String url(Language language) {
        return language.pathPrefix() + "/articles/" + slug + "/";
    }

    public String assetBase() {
        return "/articles/" + slug + "/images/";
    }
}
