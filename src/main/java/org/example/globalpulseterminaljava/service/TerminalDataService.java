package org.example.globalpulseterminaljava.service;

import org.example.globalpulseterminaljava.dto.TerminalDtos.Alert;
import org.example.globalpulseterminaljava.dto.TerminalDtos.AlertAckResponse;
import org.example.globalpulseterminaljava.dto.TerminalDtos.ConflictItem;
import org.example.globalpulseterminaljava.dto.TerminalDtos.ConflictZone;
import org.example.globalpulseterminaljava.dto.TerminalDtos.Coordinates;
import org.example.globalpulseterminaljava.dto.TerminalDtos.GlobeRoute;
import org.example.globalpulseterminaljava.dto.TerminalDtos.LiveNewsSource;
import org.example.globalpulseterminaljava.dto.TerminalDtos.NewsItem;
import org.example.globalpulseterminaljava.dto.TerminalDtos.RouteNode;
import org.example.globalpulseterminaljava.dto.TerminalDtos.SnapshotResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@Service
public class TerminalDataService {

    private final List<NewsItem> newsItems = new CopyOnWriteArrayList<>();
    private final List<ConflictItem> conflictItems = new CopyOnWriteArrayList<>();
    private final List<LiveNewsSource> sources = new CopyOnWriteArrayList<>();
    private final List<GlobeRoute> routes = new CopyOnWriteArrayList<>();
    private final List<ConflictZone> zones = new CopyOnWriteArrayList<>();
    private final List<Alert> alerts = new CopyOnWriteArrayList<>();

    public TerminalDataService() {
        seed();
    }

    @Cacheable("snapshot")
    public SnapshotResponse snapshot(String region, List<String> include) {
        long now = Instant.now().toEpochMilli();
        return new SnapshotResponse(
                queryNews(region, null, 50, null, null),
                queryConflicts(region, null, "score"),
                includeRequested(include, "sources") ? liveSources() : List.of(),
                includeRequested(include, "routes") ? routeLayers("all") : Map.of(),
                includeRequested(include, "alerts") ? queryAlerts(null, null, 25) : List.of(),
                now,
                now
        );
    }

    public List<NewsItem> queryNews(String region, String q, Integer limit, String cursor, Instant since) {
        Stream<NewsItem> stream = newsItems.stream()
                .filter(it -> regionMatches(region, it.region()))
                .filter(it -> q == null || q.isBlank() || containsIgnoreCase(it.title(), q) || containsIgnoreCase(it.summary(), q))
                .filter(it -> since == null || !it.updatedAt().isBefore(since));

        if (cursor != null && !cursor.isBlank()) {
            stream = stream.filter(it -> it.id().compareTo(cursor) > 0);
        }

        return stream
                .sorted(Comparator.comparing(NewsItem::updatedAt).reversed())
                .limit(limit == null || limit < 1 ? 20 : Math.min(limit, 100))
                .toList();
    }

    public List<ConflictItem> queryConflicts(String region, Double minScore, String sort) {
        Comparator<ConflictItem> comparator = "updatedAt".equalsIgnoreCase(sort)
                ? Comparator.comparing(ConflictItem::updatedAt).reversed()
                : Comparator.comparing(ConflictItem::score).reversed();

        return conflictItems.stream()
                .filter(it -> regionMatches(region, it.region()))
                .filter(it -> minScore == null || it.score() >= minScore)
                .sorted(comparator)
                .toList();
    }

    public List<LiveNewsSource> liveSources() {
        return List.copyOf(sources);
    }

    public List<ConflictZone> conflictZones(String region) {
        return zones.stream().filter(z -> regionMatches(region, z.region())).toList();
    }

    public Map<String, List<GlobeRoute>> routeLayers(String mode) {
        List<GlobeRoute> filtered = routes.stream()
                .filter(route -> "all".equalsIgnoreCase(mode) || route.mode().equalsIgnoreCase(mode))
                .toList();

        return Map.of(
                "flight", filtered.stream().filter(route -> "flight".equalsIgnoreCase(route.mode())).toList(),
                "shipping", filtered.stream().filter(route -> "shipping".equalsIgnoreCase(route.mode())).toList()
        );
    }

    public List<Alert> queryAlerts(String level, Instant since, Integer limit) {
        return alerts.stream()
                .filter(a -> level == null || level.isBlank() || a.level().equalsIgnoreCase(level))
                .filter(a -> since == null || !a.createdAt().isBefore(since))
                .sorted(Comparator.comparing(Alert::createdAt).reversed())
                .limit(limit == null || limit < 1 ? 20 : Math.min(limit, 100))
                .toList();
    }

    public AlertAckResponse ackAlert(String alertId, String ackBy) {
        Objects.requireNonNull(alertId, "alertId is required");
        Objects.requireNonNull(ackBy, "ackBy is required");

        Alert current = alerts.stream()
                .filter(a -> a.id().equals(alertId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        Instant ackAt = Instant.now();
        Alert acked = new Alert(current.id(), current.level(), current.message(), current.region(), current.createdAt(), "acknowledged", ackBy, ackAt);
        alerts.remove(current);
        alerts.add(acked);

        return new AlertAckResponse(alertId, acked.status(), ackBy, ackAt);
    }

    private boolean includeRequested(List<String> include, String field) {
        return include == null || include.isEmpty() || include.stream().anyMatch(f -> f.equalsIgnoreCase(field));
    }

    private boolean regionMatches(String requestedRegion, String itemRegion) {
        return requestedRegion == null || requestedRegion.isBlank() || requestedRegion.equalsIgnoreCase("global")
                || itemRegion.equalsIgnoreCase(requestedRegion);
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private void seed() {
        Instant now = Instant.now();

        newsItems.addAll(List.of(
                new NewsItem("n-101", "Ceasefire talks resume", "middle-east", "Regional mediators reopen multi-party talks.", new Coordinates(31.7683, 35.2137), now.minusSeconds(300), true),
                new NewsItem("n-102", "Shipping insurance premiums rise", "europe", "Premiums increased on key maritime corridors.", new Coordinates(50.1109, 8.6821), now.minusSeconds(1800), false),
                new NewsItem("n-103", "Pacific drills announced", "asia-pacific", "Joint military drills scheduled next week.", new Coordinates(1.3521, 103.8198), now.minusSeconds(900), false)
        ));

        conflictItems.addAll(List.of(
                new ConflictItem("c-201", "Levant Corridor", 87.2, "middle-east", now.minusSeconds(240), "Escalation risk remains elevated."),
                new ConflictItem("c-202", "Black Sea Lanes", 72.5, "europe", now.minusSeconds(450), "Intermittent disruption reported."),
                new ConflictItem("c-203", "South China Sea", 78.6, "asia-pacific", now.minusSeconds(600), "Naval activity increased over 24h.")
        ));

        sources.addAll(List.of(
                new LiveNewsSource("s-1", "Global Desk Live", "https://example.com/live/global", "https://example.com/embed/global", "24/7 geopolitics live desk"),
                new LiveNewsSource("s-2", "Maritime Watch", "https://example.com/live/maritime", "https://example.com/embed/maritime", "Shipping and choke-point live updates")
        ));

        routes.addAll(List.of(
                new GlobeRoute("r-301", new RouteNode("Dubai", 25.2048, 55.2708), new RouteNode("Athens", 37.9838, 23.7275), "flight", "DXB-ATH"),
                new GlobeRoute("r-302", new RouteNode("Singapore", 1.3521, 103.8198), new RouteNode("Rotterdam", 51.9244, 4.4777), "shipping", "SIN-RTM"),
                new GlobeRoute("r-303", new RouteNode("Doha", 25.2854, 51.5310), new RouteNode("Tokyo", 35.6762, 139.6503), "flight", "DOH-TYO")
        ));

        zones.addAll(List.of(
                new ConflictZone("Gaza Perimeter", 31.3547, 34.3088, 3, "middle-east", "High volatility and periodic closure risk."),
                new ConflictZone("Kerch Strait", 45.3046, 36.5120, 2, "europe", "Transit controls tightened."),
                new ConflictZone("Luzon Strait", 20.5000, 121.0000, 2, "asia-pacific", "Increased patrol density.")
        ));

        alerts.addAll(new ArrayList<>(List.of(
                new Alert(UUID.randomUUID().toString(), "critical", "Airspace closure advisory", "middle-east", now.minusSeconds(120), "open", null, null),
                new Alert(UUID.randomUUID().toString(), "warning", "Port delay threshold exceeded", "europe", now.minusSeconds(600), "open", null, null)
        )));
    }
}
