package com.techmart.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Runs first on every request (before Spring Security, so even 401s are logged):
 *  - gives the request an id (X-Request-Id header, reused if the client sent a safe one),
 *  - puts it in the logging MDC so EVERY log line of this request carries "requestId",
 *  - writes one structured "http_request" log line with method, path, status and duration.
 * It never logs headers, query strings or bodies, so passwords and tokens cannot leak.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    private final AppMetrics metrics;

    public RequestLoggingFilter(AppMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader("X-Request-Id");
        String requestId = (incoming != null && SAFE_ID.matcher(incoming).matches())
                ? incoming : UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);

        String method = request.getMethod();
        String path = request.getRequestURI();
        long start = System.nanoTime();
        int status = 500;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } catch (ServletException | IOException | RuntimeException ex) {
            // An exception that escaped every handler (e.g. thrown inside a filter)
            boolean database = ex instanceof DataAccessException || ex instanceof TransactionException
                    || ex.getCause() instanceof DataAccessException;
            metrics.error(database ? "database" : "unexpected");
            log.error(database ? "database_error" : "unhandled_exception",
                    kv("event", database ? "database_error" : "unhandled_exception"),
                    kv("method", method), kv("path", path), ex);
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            if (path.startsWith("/actuator")) {
                // Prometheus scrapes every few seconds: keep those lines out of the normal log level
                log.debug("http_request", kv("method", method), kv("path", path),
                        kv("status", status), kv("durationMs", durationMs));
            } else if (status >= 500) {
                log.error("http_request", kv("method", method), kv("path", path),
                        kv("status", status), kv("durationMs", durationMs));
            } else if (status >= 400) {
                log.warn("http_request", kv("method", method), kv("path", path),
                        kv("status", status), kv("durationMs", durationMs));
            } else {
                log.info("http_request", kv("method", method), kv("path", path),
                        kv("status", status), kv("durationMs", durationMs));
            }
            MDC.clear();
        }
    }
}
