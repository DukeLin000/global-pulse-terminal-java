package org.example.globalpulseterminaljava.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AppProperties appProperties;

    public RateLimitingInterceptor(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, __ -> new Bucket(Instant.now().toEpochMilli(), new AtomicInteger(0)));

        long now = Instant.now().toEpochMilli();
        synchronized (bucket) {
            if ((now - bucket.windowStartMillis) > 60_000) {
                bucket.windowStartMillis = now;
                bucket.counter.set(0);
            }

            if (bucket.counter.incrementAndGet() > appProperties.getRateLimit().getRequestsPerMinute()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(buildRateLimitJson(request.getRequestURI()));
                return false;
            }
        }

        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String buildRateLimitJson(String path) {
        String safePath = path == null ? "" : path.replace("\"", "\\\"");
        return "{" +
                "\"timestamp\":\"" + Instant.now() + "\"," +
                "\"status\":429," +
                "\"error\":\"Too Many Requests\"," +
                "\"code\":\"RATE_LIMIT_EXCEEDED\"," +
                "\"message\":\"Rate limit exceeded\"," +
                "\"path\":\"" + safePath + "\"" +
                "}";
    }

    private static final class Bucket {
        private volatile long windowStartMillis;
        private final AtomicInteger counter;

        private Bucket(long windowStartMillis, AtomicInteger counter) {
            this.windowStartMillis = windowStartMillis;
            this.counter = counter;
        }
    }
}
