package uz.syncoder.generator.read;

import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.model.LocalizedText;

import java.nio.file.Path;
import java.util.Map;

public class ArticleParser {

    private static final String META_FILE = "article.yml";

    private final ContentFiles files;
    private final Problems problems;

    public ArticleParser(ContentFiles files, Problems problems) {
        this.files = files;
        this.problems = problems;
    }

    public Article parse(Path directory) {
        String key = directory.getFileName().toString();
        try {
            return read(key, directory);
        } catch (ContentException e) {
            problems.add(key, e.getMessage());
            return null;
        }
    }

    private Article read(String key, Path directory) {
        Map<?, ?> meta = files.readMapping(directory.resolve(META_FILE));

        LocalizedText title = files.localized(meta.get("title"));
        if (title == null || !title.has(Language.RU)) {
            throw new ContentException("missing 'title' in " + META_FILE);
        }
        String slug = files.text(meta.get("slug"), key);

        return new Article(
                key,
                slug,
                title,
                files.localized(meta.get("summary")),
                files.readBody(directory, "/articles/" + slug + "/images/"),
                files.localized(meta.get("cover")),
                files.list(meta.get("tags")),
                Boolean.TRUE.equals(meta.get("published")),
                files.date(meta.get("publishedAt"), META_FILE),
                directory);
    }
}
