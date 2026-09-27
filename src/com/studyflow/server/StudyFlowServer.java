package com.studyflow.server;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/** Java 21 HTTP application. Serves the browser client and a same-origin JSON API. */
public final class StudyFlowServer {
    private final Gson json = new GsonBuilder().serializeNulls().setStrictness(Strictness.STRICT).create();
    private final AppService service;
    private final Path web;
    private final String origin;
    private final boolean secure;
    private final Map<String, Window> rateLimits = new ConcurrentHashMap<>();
    private record Window(long start, int count) { }

    private StudyFlowServer(AppService service, Path web, String origin) {
        this.service = service; this.web = web.toAbsolutePath().normalize();
        this.origin = origin; this.secure = origin.startsWith("https://");
    }
    public static void main(String[] args) throws Exception {
        boolean hosted = "true".equalsIgnoreCase(System.getenv("RENDER"));
        String host = env("STUDYFLOW_HOST", hosted ? "0.0.0.0" : "127.0.0.1");
        int port = Integer.parseInt(firstEnv("STUDYFLOW_PORT", "PORT", "8080"));
        String origin = firstEnv("STUDYFLOW_ORIGIN", "RENDER_EXTERNAL_URL",
                "http://localhost:" + port).replaceAll("/+$", "");
        URI uri = URI.create(origin);
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null
                || uri.getPath() != null && !uri.getPath().isEmpty()) throw new IllegalArgumentException("STUDYFLOW_ORIGIN must be an http(s) origin with no path.");
        Path web = Path.of(env("STUDYFLOW_WEB_DIR", "web"));
        if (!Files.isRegularFile(web.resolve("index.html"))) throw new IllegalStateException("Cannot find web/index.html. Start the server from the project folder.");
        Database db = new Database(Path.of(env("STUDYFLOW_DATA_DIR", "data")));
        String setupToken = env("STUDYFLOW_SETUP_TOKEN", Passwords.token());
        AppService service = new AppService(db, setupToken);
        StudyFlowServer app = new StudyFlowServer(service, web, origin);
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 64);
        ExecutorService executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(64), new ThreadPoolExecutor.CallerRunsPolicy());
        server.setExecutor(executor);
        server.createContext("/", app::handle);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1); executor.shutdown();
            try { db.close(); } catch (SQLException ignored) { }
        }));
        server.start();
        System.out.println("\nStudyFlow is running at " + origin);
        System.out.println("Listening on " + host + ":" + port + " | Press Ctrl+C to stop.");
        if (service.setupRequired()) {
            System.out.println("\nFirst run: open the app and create your administrator account.");
            System.out.println("Setup key: " + setupToken + "\n");
        }
    }
    private void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "same-origin");
        headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        headers.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'");
        if (secure) headers.set("Strict-Transport-Security", "max-age=31536000");
        headers.set("Cache-Control", "no-store");
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.startsWith("/api/")) {
                ApiException.require(Set.of("GET", "POST", "PUT", "DELETE").contains(method), 405, "Method not allowed.");
                String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
                if (!method.equals("GET")) {
                    ApiException.require(requestOrigin == null || requestOrigin.equals(origin), 403, "This request came from another site. Open StudyFlow at " + origin + ".");
                    ApiException.require(!"cross-site".equals(exchange.getRequestHeaders().getFirst("Sec-Fetch-Site")), 403, "Cross-site requests are not allowed.");
                }
                JsonObject body = method.equals("GET") ? new JsonObject() : readBody(exchange);
                String address = exchange.getRemoteAddress().getAddress().getHostAddress();
                if (method.equals("POST") && (path.equals("/api/auth/login") || path.equals("/api/setup") || path.equals("/api/me/password"))) {
                    limit("ip:" + address, 60);
                    if (path.equals("/api/auth/login")) limit("email:" + Inputs.optional(body, "email", "").trim().toLowerCase(Locale.ROOT), 12);
                    if (path.equals("/api/setup")) limit("setup:" + address, 12);
                }
                AppService.Reply reply = service.handle(method, path, body, cookie(exchange), exchange.getRequestHeaders().getFirst("X-CSRF-Token"));
                if (reply.sessionToken() != null) headers.add("Set-Cookie", "studyflow_session=" + reply.sessionToken()
                        + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + (reply.sessionToken().isEmpty() ? "0" : "43200") + (secure ? "; Secure" : ""));
                sendJson(exchange, reply.status(), reply.body());
            } else {
                ApiException.require(method.equals("GET") || method.equals("HEAD"), 405, "Method not allowed.");
                String file = switch (path) { case "/", "/index.html" -> "index.html"; case "/app.js" -> "app.js"; case "/styles.css" -> "styles.css"; case "/favicon.svg" -> "favicon.svg"; default -> null; };
                ApiException.require(file != null, 404, "Page not found.");
                String type = file.endsWith(".js") ? "text/javascript" : file.endsWith(".css") ? "text/css" : file.endsWith(".svg") ? "image/svg+xml" : "text/html";
                headers.set("Content-Type", type + "; charset=utf-8");
                byte[] bytes = Files.readAllBytes(web.resolve(file));
                if (method.equals("HEAD")) { exchange.sendResponseHeaders(200, -1); }
                else { exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); }
            }
        } catch (ApiException exception) {
            if (exception.status == 429) headers.set("Retry-After", "900");
            sendJson(exchange, exception.status, Map.of("error", exception.getMessage()));
        } catch (JsonParseException | IllegalStateException exception) {
            sendJson(exchange, 400, Map.of("error", "Invalid request. Check the form and try again."));
        } catch (Exception exception) {
            String errorId = UUID.randomUUID().toString().substring(0, 8);
            // Do not log request bodies, passwords, session cookies, or database parameter values.
            System.err.println("Request " + errorId + " failed: " + exception.getClass().getSimpleName());
            sendJson(exchange, 500, Map.of("error", "We could not save or load your data. Please try again. Reference: " + errorId));
        } finally { exchange.close(); }
    }
    private JsonObject readBody(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        ApiException.require(type != null && type.split(";", 2)[0].trim().equalsIgnoreCase("application/json"), 415, "Use application/json for requests.");
        byte[] bytes = exchange.getRequestBody().readNBytes(65_537);
        ApiException.require(bytes.length <= 65_536, 413, "This request is too large.");
        if (bytes.length == 0) return new JsonObject();
        JsonElement value = json.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonElement.class);
        ApiException.require(value != null && value.isJsonObject(), 400, "Request body must be a JSON object.");
        return value.getAsJsonObject();
    }
    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = json.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
    private String cookie(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().getOrDefault("Cookie", List.of());
        for (String line : cookies) for (String item : line.split(";")) {
            String[] pair = item.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals("studyflow_session") && pair[1].matches("[A-Za-z0-9_-]{43}")) return pair[1];
        }
        return null;
    }
    private synchronized void limit(String key, int maximum) {
        long now = System.currentTimeMillis(), interval = 900_000;
        rateLimits.entrySet().removeIf(entry -> now - entry.getValue().start() >= interval);
        Window current = rateLimits.get(key);
        if (current == null) {
            ApiException.require(rateLimits.size() < 10_000, 429, "Too many sign-in attempts. Try again in 15 minutes.");
            current = new Window(now, 0);
        }
        ApiException.require(current.count() < maximum, 429, "Too many sign-in attempts. Try again in 15 minutes.");
        rateLimits.put(key, new Window(current.start(), current.count() + 1));
    }
    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String firstEnv(String primary, String secondary, String fallback) {
        String value = System.getenv(primary);
        if (value != null && !value.isBlank()) return value;
        value = System.getenv(secondary);
        return value == null || value.isBlank() ? fallback : value;
    }
}
