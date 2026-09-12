package uz.syncoder.generator.serve;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class PreviewServer {

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=utf-8"),
            Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("js", "text/javascript; charset=utf-8"),
            Map.entry("json", "application/json; charset=utf-8"),
            Map.entry("xml", "application/xml; charset=utf-8"),
            Map.entry("txt", "text/plain; charset=utf-8"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("woff2", "font/woff2"));

    private final Path root;
    private final int port;

    public PreviewServer(Path root, int port) {
        this.root = root;
        this.port = port;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        String requested = URLDecoder.decode(exchange.getRequestURI().getPath(), StandardCharsets.UTF_8);
        Path file = resolve(requested);

        if (file == null) {
            respond(exchange, 404, "text/plain; charset=utf-8", notFound());
            return;
        }
        if (Files.isDirectory(file) && !requested.endsWith("/")) {
            exchange.getResponseHeaders().add("Location", requested + "/");
            respond(exchange, 301, "text/plain", new byte[0]);
            return;
        }
        respond(exchange, 200, contentType(file), Files.readAllBytes(file));
    }

    private Path resolve(String requested) {
        Path candidate = root.resolve(requested.startsWith("/") ? requested.substring(1) : requested).normalize();
        if (!candidate.startsWith(root)) {
            return null;
        }
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        if (!Files.isDirectory(candidate)) {
            return null;
        }
        if (!requested.endsWith("/")) {
            return candidate;
        }
        Path index = candidate.resolve("index.html");
        return Files.isRegularFile(index) ? index : null;
    }

    private byte[] notFound() throws IOException {
        Path page = root.resolve("404.html");
        return Files.isRegularFile(page) ? Files.readAllBytes(page) : "404".getBytes(StandardCharsets.UTF_8);
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }
}
