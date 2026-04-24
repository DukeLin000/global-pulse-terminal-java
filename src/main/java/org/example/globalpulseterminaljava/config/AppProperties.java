package org.example.globalpulseterminaljava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final RateLimit rateLimit = new RateLimit();

    public Cors getCors() {
        return cors;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://127.0.0.1:3000");

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class RateLimit {
        private int requestsPerMinute = 120;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }
}
