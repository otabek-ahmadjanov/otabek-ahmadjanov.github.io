package uz.syncoder.generator.read;

import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.model.LocalizedText;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ContentFiles {

    private static final String BODY_FILE = "content.%s.md";
    private static final String IMAGE_REF = "](images/";

    public Map<?, ?> readMapping(Path metaFile) {
        if (!Files.isRegularFile(metaFile)) {
            throw new ContentException("missing " + metaFile);
        }
        try (InputStream in = Files.newInputStream(metaFile)) {
            Object loaded = new org.yaml.snakeyaml.Yaml().load(in);
            if (loaded instanceof Map<?, ?> mapping) {
                return mapping;
            }
            throw new ContentException(metaFile + " is not a YAML mapping");
        } catch (IOException e) {
            throw new ContentException("cannot read " + metaFile, e);
        }
    }

    public LocalizedText readBody(Path directory, String imageBase) {
        return LocalizedText.of(
                readMarkdown(directory, Language.RU, imageBase),
                readMarkdown(directory, Language.EN, imageBase));
    }

    public LocalizedText localized(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return LocalizedText.of(text(map.get("ru"), null), text(map.get("en"), null));
        }
        return LocalizedText.same(text(value, null));
    }

    public List<String> list(Object value) {
        if (value instanceof List<?> items) {
            return items.stream().map(item -> text(item, null)).filter(Objects::nonNull).toList();
        }
        return List.of();
    }

    public LocalDate date(Object value, String where) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        try {
            return LocalDate.parse(text(value, ""));
        } catch (DateTimeParseException e) {
            throw new ContentException(where + ": cannot parse date '" + value + "'", e);
        }
    }

    public String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String string = value.toString().strip();
        return string.isEmpty() ? fallback : string;
    }

    private String readMarkdown(Path directory, Language language, String imageBase) {
        Path file = directory.resolve(BODY_FILE.formatted(language.code()));
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            String markdown = Files.readString(file, StandardCharsets.UTF_8);
            return imageBase == null ? markdown : markdown.replace(IMAGE_REF, "](" + imageBase);
        } catch (IOException e) {
            throw new ContentException("cannot read " + file, e);
        }
    }
}
