package uz.syncoder.generator.read;

import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.model.Page;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class ContentReader {

    private final ArticleParser articleParser;
    private final PageParser pageParser;

    public ContentReader(ArticleParser articleParser, PageParser pageParser) {
        this.articleParser = articleParser;
        this.pageParser = pageParser;
    }

    public List<Article> readArticles(Path articlesDir) {
        return read(articlesDir, articleParser::parse);
    }

    public List<Page> readPages(Path pagesDir) {
        return Files.isDirectory(pagesDir) ? read(pagesDir, pageParser::parse) : List.of();
    }

    private <T> List<T> read(Path directory, Function<Path, T> parser) {
        if (!Files.isDirectory(directory)) {
            throw new ContentException("missing content directory " + directory);
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .map(parser)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new ContentException("cannot list " + directory, e);
        }
    }
}
