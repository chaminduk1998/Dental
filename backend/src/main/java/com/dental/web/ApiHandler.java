package com.dental.web;

import com.dental.dao.DataAccessException;
import com.dental.service.AuthService;
import com.dental.util.ValidationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Base class for every REST endpoint.
 *
 * <p>Centralises exception -&gt; HTTP status mapping and session/role checks so
 * individual handlers only contain business dispatch code.</p>
 */
public abstract class ApiHandler implements HttpHandler {

    protected final AuthService authService;

    protected ApiHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (ValidationException e) {
            HttpUtil.sendError(exchange, 400, e.getMessage());
        } catch (UnauthorizedException e) {
            HttpUtil.sendError(exchange, 401, e.getMessage());
        } catch (ForbiddenException e) {
            HttpUtil.sendError(exchange, 403, e.getMessage());
        } catch (NotFoundException e) {
            HttpUtil.sendError(exchange, 404, e.getMessage());
        } catch (DataAccessException e) {
            HttpUtil.sendError(exchange, 500, e.getMessage());
        } catch (Exception e) {
            System.err.println("[api] unhandled error on " + exchange.getRequestURI() + ": " + e);
            e.printStackTrace();
            HttpUtil.sendError(exchange, 500, "Unexpected server error");
        } finally {
            exchange.close();
        }
    }

    protected abstract void route(HttpExchange exchange) throws Exception;

    /** Requires a valid session cookie; throws 401 otherwise. */
    protected AuthService.Session requireSession(HttpExchange exchange) {
        String token = HttpUtil.cookie(exchange, HttpUtil.SESSION_COOKIE);
        return authService.resolve(token)
                .orElseThrow(() -> new UnauthorizedException("Please sign in to continue"));
    }

    /** Requires a valid session belonging to an ADMIN user; throws 403 otherwise. */
    protected AuthService.Session requireAdmin(HttpExchange exchange) {
        AuthService.Session s = requireSession(exchange);
        if (!s.user.isAdmin()) {
            throw new ForbiddenException("Administrator access required");
        }
        return s;
    }

    protected boolean isMethod(HttpExchange exchange, String method) {
        return exchange.getRequestMethod().equalsIgnoreCase(method);
    }

    protected void ok(HttpExchange exchange, Object payload) throws IOException {
        HttpUtil.sendJson(exchange, 200, payload);
    }

    protected void created(HttpExchange exchange, Object payload) throws IOException {
        HttpUtil.sendJson(exchange, 201, payload);
    }

    protected void methodNotAllowed(HttpExchange exchange) throws IOException {
        HttpUtil.sendError(exchange, 405, "Method not allowed: " + exchange.getRequestMethod());
    }

    protected static JSONObject message(String msg) {
        return new JSONObject().put("message", msg);
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }
}
