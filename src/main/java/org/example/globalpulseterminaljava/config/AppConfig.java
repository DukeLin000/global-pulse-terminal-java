package org.example.globalpulseterminaljava.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("snapshot");
    }
}
