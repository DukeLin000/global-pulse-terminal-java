package org.example.globalpulseterminaljava.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final int REQUEST_LIMIT_PER_MINUTE = 120;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(key, __ -> new Bucket(Instant.now().toEpochMilli(), new AtomicInteger(0)));

        long now = Instant.now().toEpochMilli();
        synchronized (bucket) {
            if ((now - bucket.windowStartMillis) > 60_000) {
                bucket.windowStartMillis = now;
                bucket.counter.set(0);
            }

            if (bucket.counter.incrementAndGet() > REQUEST_LIMIT_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Rate limit exceeded");
                return false;
            }
        }

        return true;
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
