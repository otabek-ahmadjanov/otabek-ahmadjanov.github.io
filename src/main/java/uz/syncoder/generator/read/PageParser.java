package uz.syncoder.generator.read;

import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.model.LocalizedText;
import uz.syncoder.generator.model.Page;

import java.nio.file.Path;
import java.util.Map;

public class PageParser {

    private static final String META_FILE = "page.yml";
    private static final String DEFAULT_TEMPLATE = "page";

    private final ContentFiles files;
    private final Problems problems;

    public PageParser(ContentFiles files, Problems problems) {
        this.files = files;
        this.problems = problems;
    }

    public Page parse(Path directory) {
        String key = directory.getFileName().toString();
        try {
            return read(key, directory);
        } catch (ContentException e) {
            problems.add(key, e.getMessage());
            return null;
        }
    }

    private Page read(String key, Path directory) {
        Map<?, ?> meta = files.readMapping(directory.resolve(META_FILE));

        LocalizedText title = files.localized(meta.get("title"));
        if (title == null || !title.has(Language.RU)) {
            throw new ContentException("missing 'title' in " + META_FILE);
        }

        return new Page(
                key,
                files.text(meta.get("slug"), key),
                files.text(meta.get("template"), DEFAULT_TEMPLATE),
                title,
                files.localized(meta.get("summary")),
                files.readBody(directory, null),
                directory);
    }
}
