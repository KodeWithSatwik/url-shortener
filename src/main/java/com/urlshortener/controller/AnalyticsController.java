package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Click analytics for shortened URLs")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get click analytics for a short URL")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics retrieved"),
        @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getStats(shortCode));
    }
}
