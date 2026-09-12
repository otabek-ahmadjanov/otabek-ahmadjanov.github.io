package uz.syncoder.generator.write;

public final class Xml {

    private Xml() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public static String tag(String indent, String name, String value) {
        return value == null ? "" : indent + "<" + name + ">" + escape(value) + "</" + name + ">\n";
    }
}
