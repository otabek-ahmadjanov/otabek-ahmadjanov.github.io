package uz.syncoder.generator.write;

import uz.syncoder.generator.read.ContentException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class SiteWriter {

    private final Path outputDir;
    private final Set<String> pageUrls = new LinkedHashSet<>();

    public SiteWriter(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void writePage(String urlPath, String html) {
        pageUrls.add(urlPath);
        write(urlPath.endsWith("/") ? urlPath + "index.html" : urlPath, html);
    }

    public Set<String> pageUrls() {
        return Set.copyOf(pageUrls);
    }

    public void write(String urlPath, String content) {
        Path file = outputDir.resolve(urlPath.startsWith("/") ? urlPath.substring(1) : urlPath);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ContentException("cannot write " + file, e);
        }
    }
}
