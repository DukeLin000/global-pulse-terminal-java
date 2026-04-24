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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void snapshotShouldReturnAggregatedPayload() throws Exception {
        mockMvc.perform(get("/api/v1/terminal/snapshot")
                        .param("region", "middle-east")
                        .param("include", "routes,alerts,sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsItems").isArray())
                .andExpect(jsonPath("$.conflictItems").isArray())
                .andExpect(jsonPath("$.routeLayers.flight").isArray())
                .andExpect(jsonPath("$.alerts").isArray());
    }

    @Test
    void conflictsShouldSupportFilteringAndSort() throws Exception {
        mockMvc.perform(get("/api/v1/conflicts")
                        .param("region", "asia-pacific")
                        .param("minScore", "70")
                        .param("sort", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].region").value("asia-pacific"));
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
}
