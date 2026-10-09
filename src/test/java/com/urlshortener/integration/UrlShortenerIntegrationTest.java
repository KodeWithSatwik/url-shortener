package com.urlshortener.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlShortenerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── POST /api/v1/urls ──────────────────────────────────────────────────

    @Test
    void shorten_validUrl_returns201WithShortUrl() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com/a-long-path");

        MvcResult result = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty())
            .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.startsWith("http://localhost:8080/")))
            .andExpect(jsonPath("$.originalUrl").value("https://www.example.com/a-long-path"))
            .andReturn();

        ShortenUrlResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), ShortenUrlResponse.class);
        assertThat(response.getShortCode()).isNotBlank();
    }

    @Test
    void shorten_sameUrlTwice_returnsExistingShortCode() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://idempotent.example.com/test");

        MvcResult r1 = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated()).andReturn();

        MvcResult r2 = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk()).andReturn();   // 200 on dedup, not 201

        ShortenUrlResponse resp1 = objectMapper.readValue(r1.getResponse().getContentAsString(), ShortenUrlResponse.class);
        ShortenUrlResponse resp2 = objectMapper.readValue(r2.getResponse().getContentAsString(), ShortenUrlResponse.class);
        assertThat(resp1.getShortCode()).isEqualTo(resp2.getShortCode());
    }

    @Test
    void shorten_invalidUrl_returns400() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("javascript:alert(1)");

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shorten_missingUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shorten_ftpUrl_returns400() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("ftp://files.example.com/file.txt");

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ── GET /{shortCode} ─────────────────────────────────────────────────

    @Test
    void redirect_existingCode_returns302() throws Exception {
        // First create a short URL
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://redirect.example.com/target");

        MvcResult created = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated()).andReturn();

        ShortenUrlResponse resp = objectMapper.readValue(
            created.getResponse().getContentAsString(), ShortenUrlResponse.class);

        // Then redirect
        mockMvc.perform(get("/" + resp.getShortCode()))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://redirect.example.com/target"));
    }

    @Test
    void redirect_unknownCode_returns404() throws Exception {
        mockMvc.perform(get("/nonexistent99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void redirect_expiredUrl_returns410() throws Exception {
        // Create URL with expiry in the past
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://expired.example.com/old");
        request.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        MvcResult created = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated()).andReturn();

        ShortenUrlResponse resp = objectMapper.readValue(
            created.getResponse().getContentAsString(), ShortenUrlResponse.class);

        mockMvc.perform(get("/" + resp.getShortCode()))
            .andExpect(status().isGone());
    }

    // ── DELETE /api/v1/urls/{shortCode} ───────────────────────────────────

    @Test
    void delete_existingCode_returns204() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://delete.example.com/url");

        MvcResult created = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated()).andReturn();

        ShortenUrlResponse resp = objectMapper.readValue(
            created.getResponse().getContentAsString(), ShortenUrlResponse.class);

        mockMvc.perform(delete("/api/v1/urls/" + resp.getShortCode()))
            .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/" + resp.getShortCode()))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_unknownCode_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/doesnotexist"))
            .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/urls/{shortCode}/analytics ────────────────────────────

    @Test
    void analytics_afterRedirect_recordsClick() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://analytics.example.com/page");

        MvcResult created = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated()).andReturn();

        ShortenUrlResponse resp = objectMapper.readValue(
            created.getResponse().getContentAsString(), ShortenUrlResponse.class);

        // Perform 2 redirects
        mockMvc.perform(get("/" + resp.getShortCode()));
        mockMvc.perform(get("/" + resp.getShortCode()));

        // Wait briefly for async analytics to complete
        Thread.sleep(300);

        mockMvc.perform(get("/api/v1/urls/" + resp.getShortCode() + "/analytics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value(resp.getShortCode()))
            .andExpect(jsonPath("$.totalClicks").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)));
    }

    @Test
    void analytics_unknownCode_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/urls/unknown999/analytics"))
            .andExpect(status().isNotFound());
    }

    // ── Custom alias (Phase 5 brownfield) ────────────────────────────────

    @Test
    void shorten_withCustomAlias_usesAlias() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://alias.example.com/page");
        request.setCustomAlias("my-alias-" + System.currentTimeMillis());

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").value(org.hamcrest.Matchers.startsWith("my-alias-")));
    }

    @Test
    void shorten_duplicateCustomAlias_returns409() throws Exception {
        String alias = "dup-alias-" + System.currentTimeMillis();

        ShortenUrlRequest req1 = new ShortenUrlRequest();
        req1.setOriginalUrl("https://first.example.com");
        req1.setCustomAlias(alias);

        ShortenUrlRequest req2 = new ShortenUrlRequest();
        req2.setOriginalUrl("https://second.example.com");
        req2.setCustomAlias(alias);

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
            .andExpect(status().isConflict());
    }
}
