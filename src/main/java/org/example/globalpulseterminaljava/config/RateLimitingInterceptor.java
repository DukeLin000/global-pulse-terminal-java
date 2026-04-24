package org.example.globalpulseterminaljava.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.globalpulseterminaljava.dto.ApiErrorResponse;
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
    private final ObjectMapper objectMapper;

    public RateLimitingInterceptor(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
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
                ApiErrorResponse error = new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        "RATE_LIMIT_EXCEEDED",
                        "Rate limit exceeded",
                        request.getRequestURI()
                );
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(error));
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

    private static final class Bucket {
        private volatile long windowStartMillis;
        private final AtomicInteger counter;

        private Bucket(long windowStartMillis, AtomicInteger counter) {
            this.windowStartMillis = windowStartMillis;
            this.counter = counter;
        }
    }
}
