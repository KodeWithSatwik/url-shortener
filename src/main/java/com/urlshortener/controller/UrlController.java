package com.urlshortener.controller;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Create, redirect, and delete shortened URLs")
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;

    @Operation(summary = "Shorten a URL")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "URL shortened successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid URL or request"),
        @ApiResponse(responseCode = "409", description = "Custom alias already taken")
    })
    @PostMapping("/api/v1/urls")
    public ResponseEntity<ShortenUrlResponse> shorten(@Valid @RequestBody ShortenUrlRequest request) {
        ShortenUrlResponse response = urlShortenerService.shorten(request);
        // 200 OK when returning an existing active record (idempotent dedup); 201 Created for new records
        HttpStatus status = response.isExisting() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @Operation(summary = "Redirect to original URL")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
        @ApiResponse(responseCode = "404", description = "Short code not found"),
        @ApiResponse(responseCode = "410", description = "URL has expired")
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String originalUrl = urlShortenerService.resolve(shortCode);
        String referrer = request.getHeader(HttpHeaders.REFERER);
        String ipAddress = getClientIp(request);
        analyticsService.recordClick(shortCode, referrer, ipAddress);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")  // prevent browsers/CDNs caching the redirect
                .build();
    }

    @Operation(summary = "Delete a shortened URL")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "URL deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    @DeleteMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<Void> delete(@PathVariable String shortCode) {
        urlShortenerService.delete(shortCode);
        return ResponseEntity.noContent().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
