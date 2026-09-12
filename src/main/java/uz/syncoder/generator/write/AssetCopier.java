package uz.syncoder.generator.write;

import uz.syncoder.generator.model.Article;
import uz.syncoder.generator.read.ContentException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class AssetCopier {

    private final Path outputDir;

    public AssetCopier(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void copyStatic(Path staticDir) {
        copyTree(staticDir, outputDir);
    }

    public void copyArticleImages(Article article) {
        Path images = article.directory().resolve("images");
        if (Files.isDirectory(images)) {
            copyTree(images, outputDir.resolve("articles").resolve(article.slug()).resolve("images"));
        }
    }

    private void copyTree(Path source, Path target) {
        if (!Files.isDirectory(source)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new ContentException("cannot copy " + source + " to " + target, e);
        }
    }
}
