package uz.syncoder.generator.render;

import uz.syncoder.generator.SiteConfig;
import uz.syncoder.generator.write.SiteWriter;
import uz.syncoder.generator.write.Xml;

public class SitemapRenderer {

    private static final String SITEMAP_URL = "/sitemap.xml";

    private final SiteConfig config;
    private final SiteWriter writer;

    public SitemapRenderer(SiteConfig config, SiteWriter writer) {
        this.config = config;
        this.writer = writer;
    }

    public void render() {
        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        writer.pageUrls().stream()
                .sorted()
                .forEach(url -> xml.append("  <url>\n")
                        .append(Xml.tag("    ", "loc", config.baseUrl() + url))
                        .append("  </url>\n"));
        xml.append("</urlset>\n");

        writer.write(SITEMAP_URL, xml.toString());
        writer.write("/robots.txt", """
                User-agent: *
                Allow: /

                Sitemap: %s%s
                """.formatted(config.baseUrl(), SITEMAP_URL));
    }
}
