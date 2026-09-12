package uz.syncoder.generator;

import java.nio.file.Path;

public record SiteConfig(
        String baseUrl,
        String siteName,
        Path contentDir,
        Path templatesDir,
        Path staticDir,
        Path i18nDir,
        Path outputDir,
        int pageSize,
        boolean includeDrafts) {

    public static SiteConfig defaults(Path root) {
        return new SiteConfig(
                "https://otabek-ahmadjanov.github.io",
                "syncoder",
                root.resolve("content"),
                root.resolve("templates"),
                root.resolve("static"),
                root.resolve("i18n"),
                root.resolve("target/site"),
                5,
                false);
    }

    public SiteConfig withDrafts(boolean drafts) {
        return new SiteConfig(baseUrl, siteName, contentDir, templatesDir, staticDir,
                i18nDir, outputDir, pageSize, drafts);
    }

    public Path articlesDir() {
        return contentDir.resolve("articles");
    }

    public Path pagesDir() {
        return contentDir.resolve("pages");
    }
}
