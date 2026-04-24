package org.example.globalpulseterminaljava.api;

import org.example.globalpulseterminaljava.dto.TerminalDtos.Alert;
import org.example.globalpulseterminaljava.dto.TerminalDtos.AlertAckRequest;
import org.example.globalpulseterminaljava.dto.TerminalDtos.AlertAckResponse;
import org.example.globalpulseterminaljava.dto.TerminalDtos.ConflictItem;
import org.example.globalpulseterminaljava.dto.TerminalDtos.ConflictZone;
import org.example.globalpulseterminaljava.dto.TerminalDtos.GlobeRoute;
import org.example.globalpulseterminaljava.dto.TerminalDtos.LiveNewsSource;
import org.example.globalpulseterminaljava.dto.TerminalDtos.NewsItem;
import org.example.globalpulseterminaljava.dto.TerminalDtos.SnapshotResponse;
import org.example.globalpulseterminaljava.service.TerminalDataService;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TerminalController {

    private final TerminalDataService terminalDataService;

    public TerminalController(TerminalDataService terminalDataService) {
        this.terminalDataService = terminalDataService;
    }

    @GetMapping("/terminal/snapshot")
    public SnapshotResponse snapshot(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String include
    ) {
        List<String> includeFields = StringUtils.hasText(include) ? Arrays.stream(include.split(",")).map(String::trim).toList() : List.of();
        return terminalDataService.snapshot(region, includeFields);
    }

    @GetMapping("/news")
    public List<NewsItem> news(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Long since
    ) {
        return terminalDataService.queryNews(region, q, limit, cursor, since == null ? null : Instant.ofEpochMilli(since));
    }

    @GetMapping("/conflicts")
    public List<ConflictItem> conflicts(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Double minScore,
            @RequestParam(defaultValue = "score") String sort
    ) {
        return terminalDataService.queryConflicts(region, minScore, sort);
    }

    @GetMapping("/live-sources")
    public List<LiveNewsSource> liveSources() {
        return terminalDataService.liveSources();
    }

    @GetMapping("/globe/conflict-zones")
    public List<ConflictZone> conflictZones(@RequestParam(required = false) String region) {
        return terminalDataService.conflictZones(region);
    }

    @GetMapping("/globe/routes")
    public Map<String, List<GlobeRoute>> routes(@RequestParam(defaultValue = "all") String mode) {
        return Map.copyOf(terminalDataService.routeLayers(mode));
    }

    @GetMapping("/alerts")
    public List<Alert> alerts(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long since,
            @RequestParam(required = false) Integer limit
    ) {
        return terminalDataService.queryAlerts(level, since == null ? null : Instant.ofEpochMilli(since), limit);
    }

    @PostMapping("/alerts/ack")
    public AlertAckResponse ackAlert(@RequestBody AlertAckRequest request) {
        return terminalDataService.ackAlert(request.alertId(), request.ackBy());
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false, defaultValue = "global") String region) throws IOException {
        SseEmitter emitter = new SseEmitter(30_000L);
        emitter.send(SseEmitter.event().name("snapshot.updated").data(terminalDataService.snapshot(region, List.of())));
        emitter.send(SseEmitter.event().name("conflict.spike").data(terminalDataService.queryConflicts(region, 75.0, "score")));
        emitter.send(SseEmitter.event().name("alert.created").data(terminalDataService.queryAlerts(null, null, 1)));
        emitter.send(SseEmitter.event().name("news.breaking").data(terminalDataService.queryNews(region, null, 5, null, null).stream().filter(NewsItem::breaking).toList()));
        emitter.complete();
        return emitter;
    }
}
