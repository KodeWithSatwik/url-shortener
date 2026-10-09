package com.urlshortener.repository;

import com.urlshortener.model.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {

    Optional<ShortenedUrl> findByShortCodeAndDeletedFalse(String shortCode);

    Optional<ShortenedUrl> findByOriginalUrlAndDeletedFalse(String originalUrl);

    boolean existsByShortCode(String shortCode);

    /**
     * Hard-delete rows that are either soft-deleted or past their expiry date.
     * Called by the scheduled cleanup job. Cascade on click_events handles FK integrity.
     */
    @Modifying
    @Query("DELETE FROM ShortenedUrl u WHERE u.deleted = true OR u.expiresAt < :cutoff")
    int deleteExpiredAndDeleted(LocalDateTime cutoff);
}
