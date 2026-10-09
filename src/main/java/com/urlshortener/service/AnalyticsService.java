package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.AnalyticsResponse.ClickSummary;
import com.urlshortener.dto.AnalyticsResponse.ReferrerSummary;
import com.urlshortener.model.ClickEvent;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.exception.ShortCodeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final ShortenedUrlRepository shortenedUrlRepository;

    /**
     * Record a click event asynchronously. Failures are logged but never propagate to the caller.
     */
    @Async("analyticsExecutor")
    @Transactional
    public void recordClick(String shortCode, String referrer, String ipAddress) {
        try {
            ClickEvent event = ClickEvent.builder()
                    .shortCode(shortCode)
                    .clickedAt(LocalDateTime.now())
                    .referrer(referrer)
                    .ipAddress(ipAddress)
                    .build();
            clickEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record click for short code {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Get analytics for a short code.
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getStats(String shortCode) {
        // Verify the URL exists
        shortenedUrlRepository.findByShortCodeAndDeletedFalse(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        long totalClicks = clickEventRepository.countByShortCode(shortCode);

        List<ClickSummary> recentClicks = clickEventRepository
                .findTop20ByShortCodeOrderByClickedAtDesc(shortCode)
                .stream()
                .map(e -> ClickSummary.builder()
                        .clickedAt(e.getClickedAt())
                        .referrer(e.getReferrer())
                        .build())
                .collect(Collectors.toList());

        List<ReferrerSummary> topReferrers = clickEventRepository
                .findTopReferrersByShortCode(shortCode)
                .stream()
                .limit(5)
                .map(row -> ReferrerSummary.builder()
                        .referrer((String) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .shortCode(shortCode)
                .totalClicks(totalClicks)
                .recentClicks(recentClicks)
                .topReferrers(topReferrers)
                .build();
    }
}
