package uz.syncoder.generator;

import uz.syncoder.generator.markdown.MarkdownRenderer;
import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Language;
import uz.syncoder.generator.model.Page;
import uz.syncoder.generator.read.ArticleParser;
import uz.syncoder.generator.read.ContentException;
import uz.syncoder.generator.read.ContentFiles;
import uz.syncoder.generator.read.ContentReader;
import uz.syncoder.generator.read.ContentValidator;
import uz.syncoder.generator.read.Problems;
import uz.syncoder.generator.read.PageParser;
import uz.syncoder.generator.render.ArticleRenderer;
import uz.syncoder.generator.render.FeedRenderer;
import uz.syncoder.generator.render.IndexRenderer;
import uz.syncoder.generator.render.Messages;
import uz.syncoder.generator.render.SearchIndexRenderer;
import uz.syncoder.generator.render.SitemapRenderer;
import uz.syncoder.generator.render.TagRenderer;
import uz.syncoder.generator.render.PageRenderer;
import uz.syncoder.generator.render.StaticPageRenderer;
import uz.syncoder.generator.render.Templates;
import uz.syncoder.generator.render.ViewFactory;
import uz.syncoder.generator.serve.ContentWatcher;
import uz.syncoder.generator.serve.PreviewServer;
import uz.syncoder.generator.write.AssetCopier;
import uz.syncoder.generator.write.SiteWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Generator {

    private final SiteConfig config;

    public Generator(SiteConfig config) {
        this.config = config;
    }

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        List<String> options = List.of(args);
        SiteConfig config = SiteConfig.defaults(Path.of("").toAbsolutePath())
                .withDrafts(options.contains("--drafts"));
        Generator generator = new Generator(config);

        if (options.contains("--serve")) {
            generator.serve(port(options));
            return;
        }
        try {
            generator.build();
        } catch (ContentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static int port(List<String> options) {
        int flag = options.indexOf("--port");
        return flag >= 0 && flag + 1 < options.size() ? Integer.parseInt(options.get(flag + 1)) : DEFAULT_PORT;
    }

    public void serve(int port) throws Exception {
        rebuild();
        new PreviewServer(config.outputDir(), port).start();
        System.out.printf("Preview on http://localhost:%d — watching for changes, Ctrl+C to stop%n", port);
        new ContentWatcher(List.of(
                config.contentDir(), config.templatesDir(), config.staticDir(), config.i18nDir()))
                .watch(this::rebuild);
    }

    private void rebuild() {
        try {
            build();
        } catch (ContentException e) {
            System.err.println(e.getMessage());
        }
    }

    public void build() {
        long startedAt = System.currentTimeMillis();
        clean(config.outputDir());

        Problems problems = new Problems();
        ContentFiles files = new ContentFiles();
        ContentReader reader = new ContentReader(
                new ArticleParser(files, problems), new PageParser(files, problems));
        List<Article> articles = publishedArticles(reader.readArticles(config.articlesDir()));
        List<Page> pages = reader.readPages(config.pagesDir());

        new ContentValidator().validate(articles, pages, problems);
        if (problems.any()) {
            throw new ContentException(problems.report());
        }

        Messages messages = new Messages(config.i18nDir());
        Templates templates = new Templates(config.templatesDir(), messages);
        MarkdownRenderer markdown = new MarkdownRenderer();
        ViewFactory views = new ViewFactory(markdown);
        SiteWriter writer = new SiteWriter(config.outputDir());
        AssetCopier assets = new AssetCopier(config.outputDir());

        ArticleRenderer articleRenderer = new ArticleRenderer(config, templates, views, writer);
        IndexRenderer indexRenderer = new IndexRenderer(config, templates, views, writer);
        PageRenderer pageRenderer = new PageRenderer(config, templates, views, writer);
        StaticPageRenderer staticPageRenderer = new StaticPageRenderer(config, templates, writer);
        TagRenderer tagRenderer = new TagRenderer(config, templates, views, writer);
        SearchIndexRenderer searchIndexRenderer = new SearchIndexRenderer(markdown, writer);
        FeedRenderer feedRenderer = new FeedRenderer(config, messages, writer);
        SitemapRenderer sitemapRenderer = new SitemapRenderer(config, writer);

        int written = 0;
        for (Article article : articles) {
            for (Language language : Language.values()) {
                if (article.hasTranslation(language)) {
                    articleRenderer.render(article, language);
                    written++;
                }
            }
            assets.copyArticleImages(article);
        }
        for (Page page : pages) {
            for (Language language : Language.values()) {
                if (page.hasTranslation(language)) {
                    pageRenderer.render(page, language);
                    written++;
                }
            }
        }
        for (Language language : Language.values()) {
            written += indexRenderer.render(articles, language);
            written += tagRenderer.render(articles, language);
            searchIndexRenderer.render(articles, language);
            feedRenderer.render(articles, language);
            staticPageRenderer.renderPage("search", language.pathPrefix() + "/search/", language,
                    "search.title", language.other().pathPrefix() + "/search/");
            written++;
        }
        staticPageRenderer.renderFile("404", "/404.html", Language.RU);
        written++;

        sitemapRenderer.render();

        assets.copyStatic(config.staticDir());

        System.out.printf("Built %d pages from %d articles and %d pages in %d ms -> %s%n",
                written, articles.size(), pages.size(), System.currentTimeMillis() - startedAt, config.outputDir());
    }

    private List<Article> publishedArticles(List<Article> articles) {
        return articles.stream()
                .filter(article -> config.includeDrafts() || article.published())
                .sorted(Comparator.comparing(Article::publishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void clean(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new ContentException("cannot clean " + directory, e);
        }
    }
}
