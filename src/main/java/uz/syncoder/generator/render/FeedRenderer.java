package uz.syncoder.generator.render;

import uz.syncoder.generator.SiteConfig;
import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.write.SiteWriter;
import uz.syncoder.generator.write.Xml;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class FeedRenderer {

    private static final int MAX_ITEMS = 20;
    private static final DateTimeFormatter RFC_822 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final SiteConfig config;
    private final Messages messages;
    private final SiteWriter writer;

    public FeedRenderer(SiteConfig config, Messages messages, SiteWriter writer) {
        this.config = config;
        this.messages = messages;
        this.writer = writer;
    }

    public static String url(Language language) {
        return language.pathPrefix() + "/feed.xml";
    }

    public void render(List<Article> articles, Language language) {
        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n")
                .append("  <channel>\n")
                .append(Xml.tag("    ", "title", config.siteName()))
                .append(Xml.tag("    ", "link", config.baseUrl() + language.pathPrefix() + "/"))
                .append(Xml.tag("    ", "description", messages.get(language, "site.description")))
                .append(Xml.tag("    ", "language", language.code()))
                .append("    <atom:link rel=\"self\" type=\"application/rss+xml\" href=\"")
                .append(Xml.escape(config.baseUrl() + url(language)))
                .append("\"/>\n");

        articles.stream()
                .filter(article -> article.hasTranslation(language))
                .limit(MAX_ITEMS)
                .forEach(article -> appendItem(xml, article, language));

        xml.append("  </channel>\n</rss>\n");
        writer.write(url(language), xml.toString());
    }

    private void appendItem(StringBuilder xml, Article article, Language language) {
        String link = config.baseUrl() + article.url(language);
        xml.append("    <item>\n")
                .append(Xml.tag("      ", "title", article.title().resolve(language).replace('\n', ' ')))
                .append(Xml.tag("      ", "link", link))
                .append("      <guid isPermaLink=\"true\">").append(Xml.escape(link)).append("</guid>\n")
                .append(Xml.tag("      ", "pubDate", published(article.publishedAt())))
                .append(Xml.tag("      ", "description",
                        article.summary() == null ? null : article.summary().resolve(language)))
                .append("    </item>\n");
    }

    private String published(LocalDate date) {
        return date == null ? null : RFC_822.format(date.atStartOfDay(ZoneOffset.UTC));
    }
}
