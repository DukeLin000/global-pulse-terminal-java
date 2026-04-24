package org.example.globalpulseterminaljava.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TerminalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("global-pulse-terminal-java"));
    }

    @Test
    void snapshotShouldReturnStablePayloadShape() throws Exception {
        mockMvc.perform(get("/api/v1/terminal/snapshot")
                        .param("region", "middle-east")
                        .param("include", "routes,alerts,sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsItems").isArray())
                .andExpect(jsonPath("$.conflictItems").isArray())
                .andExpect(jsonPath("$.liveNewsSources").isArray())
                .andExpect(jsonPath("$.routeLayers.flight").isArray())
                .andExpect(jsonPath("$.routeLayers.shipping").isArray())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.serverTime").isNumber())
                .andExpect(jsonPath("$.fetchedAt").isNumber());
    }

    @Test
    void newsShouldSupportRegionFiltering() throws Exception {
        mockMvc.perform(get("/api/v1/news").param("region", "middle-east"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].region").value("middle-east"));
    }

    @Test
    void conflictsShouldSupportFilteringAndSort() throws Exception {
        mockMvc.perform(get("/api/v1/conflicts")
                        .param("minScore", "70")
                        .param("sort", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").isNumber());
    }

    @Test
    void routesShouldSupportModeFilter() throws Exception {
        mockMvc.perform(get("/api/v1/globe/routes").param("mode", "flight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flight").isArray())
                .andExpect(jsonPath("$.shipping").isArray());
    }

    @Test
    void alertsShouldReturnWithLimit() throws Exception {
        mockMvc.perform(get("/api/v1/alerts").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldAcknowledgeAlert() throws Exception {
        String alertsPayload = mockMvc.perform(get("/api/v1/alerts").param("limit", "1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode alerts = objectMapper.readTree(alertsPayload);
        String alertId = alerts.get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/alerts/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alertId\":\"" + alertId + "\",\"ackBy\":\"ops-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value(alertId))
                .andExpect(jsonPath("$.status").value("acknowledged"));
    }

    @Test
    void invalidAlertAckShouldReturnApiErrorResponse() throws Exception {
        mockMvc.perform(post("/api/v1/alerts/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alertId\":\"unknown\",\"ackBy\":\"ops-user\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/alerts/ack"));
    }

    @Test
    void invalidQueryParamTypeShouldReturnApiErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/conflicts").param("minScore", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/conflicts"));
    }

    @Test
    void corsPreflightShouldPass() throws Exception {
        mockMvc.perform(options("/api/v1/news")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization,Accept"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
