package uz.syncoder.generator.write;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Json {

    private Json() {
    }

    public static String object(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(entry -> quote(entry.getKey()) + ":" + entry.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    public static String array(List<String> values) {
        return values.stream().collect(Collectors.joining(",", "[", "]"));
    }

    public static String strings(List<String> values) {
        return array(values.stream().map(Json::quote).toList());
    }

    public static String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
