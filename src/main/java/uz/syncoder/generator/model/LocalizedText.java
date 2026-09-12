package uz.syncoder.generator.model;

public record LocalizedText(String ru, String en) {

    public static LocalizedText of(String ru, String en) {
        return new LocalizedText(ru, en);
    }

    public static LocalizedText same(String value) {
        return new LocalizedText(value, value);
    }

    public String get(Language language) {
        return language == Language.EN ? en : ru;
    }

    public String resolve(Language language) {
        String value = get(language);
        return isBlank(value) ? ru : value;
    }

    public boolean has(Language language) {
        return !isBlank(get(language));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
