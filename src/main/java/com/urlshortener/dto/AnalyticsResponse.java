package com.urlshortener.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AnalyticsResponse {

    private String shortCode;
    private long totalClicks;
    private List<ClickSummary> recentClicks;
    private List<ReferrerSummary> topReferrers;

    @Data
    @Builder
    public static class ClickSummary {
        private LocalDateTime clickedAt;
        private String referrer;
    }

    @Data
    @Builder
    public static class ReferrerSummary {
        private String referrer;
        private long count;
    }
}
