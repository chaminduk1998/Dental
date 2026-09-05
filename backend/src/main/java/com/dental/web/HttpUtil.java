package com.dental.web;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small helpers shared by every HTTP handler - body/query parsing, JSON output, cookies. */
public final class HttpUtil {

    public static final String SESSION_COOKIE = "dental_session";

    private HttpUtil() { }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            in.transferTo(buf);
            return buf.toString(StandardCharsets.UTF_8);
        }
    }

    public static JSONObject readJson(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        if (body == null || body.isBlank()) {
            return new JSONObject();
        }
        return new JSONObject(new JSONTokener(body));
    }

    public static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            if (pair.isBlank()) continue;
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            params.put(decode(key), decode(value));
        }
        return params;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    public static String[] pathSegments(HttpExchange exchange, String prefix) {
        String path = exchange.getRequestURI().getPath();
        String rest = path.startsWith(prefix) ? path.substring(prefix.length()) : path;
        rest = rest.replaceAll("^/+", "").replaceAll("/+$", "");
        return rest.isEmpty() ? new String[0] : rest.split("/");
    }

    public static void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        String body = payload instanceof JSONObject j ? j.toString()
                : payload instanceof JSONArray a ? a.toString()
                : String.valueOf(payload);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, new JSONObject().put("error", message).put("status", status));
    }

    public static String cookie(HttpExchange exchange, String name) {
        String header = exchange.getRequestHeaders().getFirst("Cookie");
        if (header == null) {
            return null;
        }
        for (String part : header.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return kv[1];
            }
        }
        return null;
    }

    public static void setCookie(HttpExchange exchange, String name, String value, int maxAgeSeconds) {
        String cookie = name + "=" + value + "; Path=/; HttpOnly; SameSite=Lax"
                + (maxAgeSeconds >= 0 ? "; Max-Age=" + maxAgeSeconds : "");
        exchange.getResponseHeaders().add("Set-Cookie", cookie);
    }

    public static void clearCookie(HttpExchange exchange, String name) {
        setCookie(exchange, name, "", 0);
    }

    public static int intOr(String s, int fallback) {
        try {
            return s == null ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static double doubleOr(Object o, double fallback) {
        if (o == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String str(JSONObject json, String key) {
        return json.has(key) && !json.isNull(key) ? String.valueOf(json.get(key)) : null;
    }

    public static int intField(JSONObject json, String key, int fallback) {
        return json.has(key) && !json.isNull(key) ? json.optInt(key, fallback) : fallback;
    }

    public static double doubleField(JSONObject json, String key, double fallback) {
        return json.has(key) && !json.isNull(key) ? json.optDouble(key, fallback) : fallback;
    }
}
