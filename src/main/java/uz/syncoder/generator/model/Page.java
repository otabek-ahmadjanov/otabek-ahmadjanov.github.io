package uz.syncoder.generator.model;

import java.nio.file.Path;

public record Page(
        String key,
        String slug,
        String template,
        LocalizedText title,
        LocalizedText summary,
        LocalizedText body,
        Path directory) {

    public boolean hasTranslation(Language language) {
        return body != null && body.has(language);
    }

    public String url(Language language) {
        return language.pathPrefix() + "/" + slug + "/";
    }
}
