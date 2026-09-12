package uz.syncoder.generator.render;

import uz.syncoder.generator.SiteConfig;
import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.render.view.ArticleView;
import uz.syncoder.generator.render.view.PaginationView;
import uz.syncoder.generator.write.SiteWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexRenderer {

    private final SiteConfig config;
    private final Templates templates;
    private final ViewFactory views;
    private final SiteWriter writer;

    public IndexRenderer(SiteConfig config, Templates templates, ViewFactory views, SiteWriter writer) {
        this.config = config;
        this.templates = templates;
        this.views = views;
        this.writer = writer;
    }

    public int render(List<Article> articles, Language language) {
        List<Article> visible = articles.stream()
                .filter(article -> article.hasTranslation(language))
                .toList();
        int totalPages = Math.max(1, (visible.size() + config.pageSize() - 1) / config.pageSize());

        for (int page = 1; page <= totalPages; page++) {
            int from = (page - 1) * config.pageSize();
            List<ArticleView> pageArticles = visible.subList(from, Math.min(from + config.pageSize(), visible.size()))
                    .stream()
                    .map(article -> views.article(article, language))
                    .toList();

            Map<String, Object> model = new HashMap<>();
            model.put("articles", pageArticles);
            model.put("pagination", new PaginationView(page, totalPages, pageUrl(language, page - 1),
                            page < totalPages ? pageUrl(language, page + 1) : null));
            model.put("lang", language.code());
            model.put("altUrl", homeUrl(language.other()));
            model.put("currentPath", pageUrl(language, page));
            model.put("siteBaseUrl", config.baseUrl());
            model.put("siteName", config.siteName());

            writer.writePage(pageUrl(language, page), templates.render("index", language, model));
        }
        return totalPages;
    }

    private String homeUrl(Language language) {
        return language.pathPrefix() + "/";
    }

    private String pageUrl(Language language, int page) {
        if (page < 1) {
            return null;
        }
        return page == 1 ? homeUrl(language) : language.pathPrefix() + "/page/" + page + "/";
    }
}
