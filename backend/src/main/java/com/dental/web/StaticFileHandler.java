package com.dental.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves the static frontend (HTML/CSS/JS) from {@code web.root}.
 * Falls back to {@code index.html} for any unknown path so the single-page
 * app can own client-side routing, and refuses to serve outside the web root.
 */
public class StaticFileHandler implements HttpHandler {

    private final Path root;

    public StaticFileHandler(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            Path candidate = root.resolve(path.substring(1)).normalize();

            if (!candidate.startsWith(root) || !Files.exists(candidate) || Files.isDirectory(candidate)) {
                candidate = root.resolve("index.html");
            }

            byte[] bytes = Files.readAllBytes(candidate);
            exchange.getResponseHeaders().set("Content-Type", contentType(candidate.toString()));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            String msg = "Static file error: " + e.getMessage();
            byte[] bytes = msg.getBytes();
            exchange.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } finally {
            exchange.close();
        }
    }

    private String contentType(String file) {
        String f = file.toLowerCase();
        if (f.endsWith(".html")) return "text/html; charset=UTF-8";
        if (f.endsWith(".css")) return "text/css; charset=UTF-8";
        if (f.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (f.endsWith(".json")) return "application/json; charset=UTF-8";
        if (f.endsWith(".svg")) return "image/svg+xml";
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".ico")) return "image/x-icon";
        if (f.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }
}
