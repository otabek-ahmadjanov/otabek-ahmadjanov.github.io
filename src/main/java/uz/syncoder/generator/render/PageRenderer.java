package uz.syncoder.generator.render;

import uz.syncoder.generator.SiteConfig;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.model.Page;
import uz.syncoder.generator.render.view.PageView;
import uz.syncoder.generator.write.SiteWriter;

import java.util.HashMap;
import java.util.Map;

public class PageRenderer {

    private final SiteConfig config;
    private final Templates templates;
    private final ViewFactory views;
    private final SiteWriter writer;

    public PageRenderer(SiteConfig config, Templates templates, ViewFactory views, SiteWriter writer) {
        this.config = config;
        this.templates = templates;
        this.views = views;
        this.writer = writer;
    }

    public void render(Page page, Language language) {
        PageView view = views.page(page, language);
        Map<String, Object> model = new HashMap<>();
        model.put("page", view);
        model.put("lang", language.code());
        model.put("altUrl", altUrl(page, language));
        model.put("currentPath", view.url());
        model.put("pageTitle", view.title());
        model.put("metaDescription", view.summary());
        model.put("siteBaseUrl", config.baseUrl());
        model.put("siteName", config.siteName());

        writer.writePage(view.url(), templates.render(page.template(), language, model));
    }

    private String altUrl(Page page, Language language) {
        Language other = language.other();
        return page.hasTranslation(other) ? page.url(other) : null;
    }
}
