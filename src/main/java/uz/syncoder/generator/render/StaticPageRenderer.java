package uz.syncoder.generator.render;

import uz.syncoder.generator.SiteConfig;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.write.SiteWriter;

import java.util.HashMap;
import java.util.Map;

public class StaticPageRenderer {

    private final SiteConfig config;
    private final Templates templates;
    private final SiteWriter writer;

    public StaticPageRenderer(SiteConfig config, Templates templates, SiteWriter writer) {
        this.config = config;
        this.templates = templates;
        this.writer = writer;
    }

    public void renderFile(String template, String outputPath, Language language) {
        writer.write(outputPath, templates.render(template, language, baseModel(language)));
    }

    public void renderPage(String template, String url, Language language, String titleKey, String altUrl) {
        Map<String, Object> model = baseModel(language);
        model.put("currentPath", url);
        model.put("pageTitle", templates.messages().get(language, titleKey));
        model.put("altUrl", altUrl);
        writer.writePage(url, templates.render(template, language, model));
    }

    private Map<String, Object> baseModel(Language language) {
        Map<String, Object> model = new HashMap<>();
        model.put("lang", language.code());
        model.put("siteBaseUrl", config.baseUrl());
        model.put("siteName", config.siteName());
        return model;
    }
}
