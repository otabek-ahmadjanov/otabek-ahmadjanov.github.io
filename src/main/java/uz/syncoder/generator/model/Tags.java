package uz.syncoder.generator.model;

import java.util.Locale;

public final class Tags {

    private Tags() {
    }

    public static String slug(String tag) {
        return tag.strip().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    public static String indexUrl(Language language) {
        return language.pathPrefix() + "/tags/";
    }

    public static String url(Language language, String tag) {
        return indexUrl(language) + slug(tag) + "/";
    }
}
