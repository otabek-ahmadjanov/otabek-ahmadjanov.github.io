package uz.syncoder.generator.markdown;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownRenderer {

    private static final int WORDS_PER_MINUTE = 200;
    private static final Pattern HEADING = Pattern.compile("^#{2,3}\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern INLINE_MARKUP = Pattern.compile("[`*_]");

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        List<Extension> extensions = List.of(
                TablesExtension.create(),
                AutolinkExtension.create(),
                HeadingAnchorExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    public String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return renderer.render(parser.parse(markdown));
    }

    public List<String> headings(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        List<String> headings = new java.util.ArrayList<>();
        Matcher matcher = HEADING.matcher(markdown);
        while (matcher.find()) {
            headings.add(INLINE_MARKUP.matcher(matcher.group(1)).replaceAll("").strip());
        }
        return List.copyOf(headings);
    }

    public int readingMinutes(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return 1;
        }
        int words = markdown.trim().split("\\s+").length;
        return Math.max(1, Math.round((float) words / WORDS_PER_MINUTE));
    }
}
