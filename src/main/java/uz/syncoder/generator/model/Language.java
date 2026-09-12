package uz.syncoder.generator.model;

import java.util.Locale;

public enum Language {

    RU("ru", "", Locale.forLanguageTag("ru")),
    EN("en", "/en", Locale.ENGLISH);

    private final String code;
    private final String pathPrefix;
    private final Locale locale;

    Language(String code, String pathPrefix, Locale locale) {
        this.code = code;
        this.pathPrefix = pathPrefix;
        this.locale = locale;
    }

    public String code() {
        return code;
    }

    public String pathPrefix() {
        return pathPrefix;
    }

    public Locale locale() {
        return locale;
    }

    public Language other() {
        return this == RU ? EN : RU;
    }
}
