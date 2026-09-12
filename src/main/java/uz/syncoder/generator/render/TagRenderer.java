package uz.syncoder.generator.render;

import uz.syncoder.generator.SiteConfig;
import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.model.Tags;
import uz.syncoder.generator.render.view.ArticleView;
import uz.syncoder.generator.render.view.TagView;
import uz.syncoder.generator.write.SiteWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TagRenderer {

    private final SiteConfig config;
    private final Templates templates;
    private final ViewFactory views;
    private final SiteWriter writer;

    public TagRenderer(SiteConfig config, Templates templates, ViewFactory views, SiteWriter writer) {
        this.config = config;
        this.templates = templates;
        this.views = views;
        this.writer = writer;
    }

    public int render(List<Article> articles, Language language) {
        Map<String, List<Article>> byTag = groupByTag(articles, language);

        List<TagView> tags = byTag.entrySet().stream()
                .map(entry -> new TagView(entry.getKey(), Tags.url(language, entry.getKey()), entry.getValue().size()))
                .toList();
        writer.writePage(Tags.indexUrl(language),
                templates.render("tags", language, indexModel(tags, language)));

        byTag.forEach((tag, tagged) -> writer.writePage(Tags.url(language, tag),
                templates.render("tag", language, tagModel(tag, tagged, articles, language))));

        return byTag.size() + 1;
    }

    private Map<String, List<Article>> groupByTag(List<Article> articles, Language language) {
        Map<String, List<Article>> byTag = new TreeMap<>();
        for (Article article : articles) {
            if (article.hasTranslation(language)) {
                article.tags().forEach(tag -> byTag.computeIfAbsent(tag, key -> new ArrayList<>()).add(article));
            }
        }
        return byTag;
    }

    private Map<String, Object> indexModel(List<TagView> tags, Language language) {
        Map<String, Object> model = baseModel(language, Tags.indexUrl(language));
        model.put("tags", tags);
        model.put("pageTitle", templates.messages().get(language, "tags.title"));
        model.put("altUrl", Tags.indexUrl(language.other()));
        return model;
    }

    private Map<String, Object> tagModel(String tag, List<Article> tagged, List<Article> all, Language language) {
        Map<String, Object> model = baseModel(language, Tags.url(language, tag));
        model.put("tag", tag);
        model.put("pageTitle", templates.messages().get(language, "tag.title", tag));
        model.put("articles", tagged.stream().map(article -> views.article(article, language)).toList());
        model.put("altUrl", existsIn(all, tag, language.other()) ? Tags.url(language.other(), tag) : null);
        return model;
    }

    private boolean existsIn(List<Article> articles, String tag, Language language) {
        return articles.stream().anyMatch(article -> article.hasTranslation(language) && article.tags().contains(tag));
    }

    private Map<String, Object> baseModel(Language language, String path) {
        Map<String, Object> model = new HashMap<>();
        model.put("lang", language.code());
        model.put("currentPath", path);
        model.put("siteBaseUrl", config.baseUrl());
        model.put("siteName", config.siteName());
        return model;
    }
}
