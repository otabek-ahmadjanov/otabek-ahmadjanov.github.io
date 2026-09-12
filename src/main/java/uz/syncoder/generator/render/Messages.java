package uz.syncoder.generator.render;

import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.read.ContentException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public class Messages {

    private final Map<Language, Properties> byLanguage = new EnumMap<>(Language.class);

    public Messages(Path i18nDir) {
        for (Language language : Language.values()) {
            byLanguage.put(language, load(i18nDir.resolve("messages_" + language.code() + ".properties")));
        }
    }

    public String get(Language language, String key, Object... parameters) {
        String pattern = byLanguage.get(language).getProperty(key);
        if (pattern == null) {
            return null;
        }
        return parameters.length == 0
                ? pattern
                : new MessageFormat(pattern, language.locale()).format(parameters);
    }

    private Properties load(Path file) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new ContentException("cannot read " + file, e);
        }
        return properties;
    }
}
