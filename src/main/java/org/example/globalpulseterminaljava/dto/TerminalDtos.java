package org.example.globalpulseterminaljava.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class TerminalDtos {

    private TerminalDtos() {
    }

    public record Coordinates(double lat, double lon) {
    }

    public record NewsItem(
            String id,
            String title,
            String region,
            String summary,
            Coordinates focus,
            Instant updatedAt,
            boolean breaking
    ) {
    }

    public record ConflictItem(
            String id,
            String name,
            double score,
            String region,
            Instant updatedAt,
            String detail
    ) {
    }

    public record LiveNewsSource(
            String id,
            String label,
            String url,
            String embedUrl,
            String description
    ) {
    }

    public record RouteNode(String label, double lat, double lon) {
    }

    public record GlobeRoute(
            String id,
            RouteNode from,
            RouteNode to,
            String mode,
            String label
    ) {
    }

    public record ConflictZone(
            String name,
            double lat,
            double lon,
            int tier,
            String region,
            String detail
    ) {
    }

    public record Alert(
            String id,
            String level,
            String message,
            String region,
            Instant createdAt,
            String status,
            String ackBy,
            Instant ackAt
    ) {
    }

    public record AlertAckRequest(String alertId, String ackBy) {
    }

    public record AlertAckResponse(String alertId, String status, String ackBy, Instant ackAt) {
    }

    public record SnapshotResponse(
            List<NewsItem> newsItems,
            List<ConflictItem> conflictItems,
            List<LiveNewsSource> liveNewsSources,
            Map<String, List<GlobeRoute>> routeLayers,
            List<Alert> alerts,
            long serverTime,
            long fetchedAt
    ) {
    }

    public record StreamEvent(String event, Object payload, long serverTime) {
    }
}
