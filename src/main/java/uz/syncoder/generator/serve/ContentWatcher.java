package uz.syncoder.generator.serve;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ContentWatcher {

    private static final long DEBOUNCE_MILLIS = 150;

    private final List<Path> roots;

    public ContentWatcher(List<Path> roots) {
        this.roots = roots;
    }

    public void watch(Runnable onChange) throws IOException, InterruptedException {
        try (WatchService service = roots.get(0).getFileSystem().newWatchService()) {
            for (Path root : roots) {
                registerTree(service, root);
            }
            while (true) {
                WatchKey key = service.take();
                drain(service, key);
                onChange.run();
                for (Path root : roots) {
                    registerTree(service, root);
                }
            }
        }
    }

    private void drain(WatchService service, WatchKey first) throws InterruptedException {
        WatchKey key = first;
        while (key != null) {
            key.pollEvents();
            key.reset();
            key = service.poll(DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private void registerTree(WatchService service, Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                directory.register(service,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
