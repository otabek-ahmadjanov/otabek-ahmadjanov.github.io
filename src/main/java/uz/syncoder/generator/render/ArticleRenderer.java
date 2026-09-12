package uz.syncoder.generator.render;

import uz.syncoder.generator.SiteConfig;
import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.render.view.ArticleView;
import uz.syncoder.generator.write.SiteWriter;

import java.util.HashMap;
import java.util.Map;

public class ArticleRenderer {

    private final SiteConfig config;
    private final Templates templates;
    private final ViewFactory views;
    private final SiteWriter writer;

    public ArticleRenderer(SiteConfig config, Templates templates, ViewFactory views, SiteWriter writer) {
        this.config = config;
        this.templates = templates;
        this.views = views;
        this.writer = writer;
    }

    public void render(Article article, Language language) {
        ArticleView view = views.article(article, language);
        Map<String, Object> model = new HashMap<>();
        model.put("article", view);
        model.put("lang", language.code());
        model.put("altUrl", altUrl(article, language));
        model.put("currentPath", view.url());
        model.put("pageTitle", view.title());
        model.put("metaDescription", view.summary());
        model.put("ogImage", view.coverImage());
        model.put("ogType", "article");
        model.put("siteBaseUrl", config.baseUrl());
        model.put("siteName", config.siteName());

        writer.writePage(view.url(), templates.render("article", language, model));
    }

    private String altUrl(Article article, Language language) {
        Language other = language.other();
        return article.hasTranslation(other) ? article.url(other) : null;
    }
}
