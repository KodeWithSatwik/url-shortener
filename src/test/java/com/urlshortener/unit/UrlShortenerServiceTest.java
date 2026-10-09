package com.urlshortener.unit;

import com.urlshortener.config.AppConfig;
import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.exception.AliasAlreadyTakenException;
import com.urlshortener.exception.ShortCodeNotFoundException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.model.ShortenedUrl;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.service.Base62Encoder;
import com.urlshortener.service.UrlShortenerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlShortenerServiceTest {

    @Mock
    private ShortenedUrlRepository repository;

    @Mock
    private Base62Encoder encoder;

    @Mock
    private AppConfig appConfig;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UrlShortenerService service;

    private AppConfig.ShortCode shortCodeConfig;

    @BeforeEach
    void setUp() {
        shortCodeConfig = new AppConfig.ShortCode();
        shortCodeConfig.setLength(6);
        shortCodeConfig.setMaxRetries(5);

        lenient().when(appConfig.getShortCode()).thenReturn(shortCodeConfig);
        lenient().when(appConfig.getDefaultTtlDays()).thenReturn(30);
        lenient().when(appConfig.getBaseUrl()).thenReturn("http://localhost:8080");
    }

    @Test
    void shorten_newUrl_savesAndReturnsResponse() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://example.com");

        when(repository.findByOriginalUrlAndDeletedFalse("https://example.com"))
            .thenReturn(Optional.empty());

        // Stub EntityManager sequence query
        Query mockQuery = mock(Query.class);
        when(entityManager.createNativeQuery("SELECT nextval('short_code_seq')")).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(1L);
        when(encoder.encode(1L)).thenReturn("abc123");
        when(repository.existsByShortCode("abc123")).thenReturn(false);

        ShortenedUrl saved = ShortenedUrl.builder()
            .id(1L).shortCode("abc123").originalUrl("https://example.com")
            .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(30))
            .build();
        when(repository.save(any())).thenReturn(saved);

        ShortenUrlResponse response = service.shorten(request);

        assertThat(response.getShortCode()).isEqualTo("abc123");
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/abc123");
    }

    @Test
    void shorten_existingUrl_returnsExistingCode() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://example.com");

        ShortenedUrl existing = ShortenedUrl.builder()
            .id(1L).shortCode("abc123").originalUrl("https://example.com")
            .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(30))
            .build();

        when(repository.findByOriginalUrlAndDeletedFalse("https://example.com"))
            .thenReturn(Optional.of(existing));

        ShortenUrlResponse response = service.shorten(request);

        assertThat(response.getShortCode()).isEqualTo("abc123");
        verify(repository, never()).save(any()); // no new save
    }

    @Test
    void shorten_customAlias_usesAlias() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("my-link");

        when(repository.existsByShortCode("my-link")).thenReturn(false);
        ShortenedUrl saved = ShortenedUrl.builder()
            .id(2L).shortCode("my-link").originalUrl("https://example.com")
            .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(30))
            .build();
        when(repository.save(any())).thenReturn(saved);

        ShortenUrlResponse response = service.shorten(request);

        assertThat(response.getShortCode()).isEqualTo("my-link");
    }

    @Test
    void shorten_customAliasTaken_throwsAliasAlreadyTaken() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("taken-alias");

        when(repository.existsByShortCode("taken-alias")).thenReturn(true);

        assertThatThrownBy(() -> service.shorten(request))
            .isInstanceOf(AliasAlreadyTakenException.class)
            .hasMessageContaining("taken-alias");
    }

    @Test
    void resolve_existingActiveUrl_returnsOriginalUrl() {
        ShortenedUrl url = ShortenedUrl.builder()
            .id(1L).shortCode("abc123").originalUrl("https://example.com")
            .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(30))
            .build();

        when(repository.findByShortCodeAndDeletedFalse("abc123"))
            .thenReturn(Optional.of(url));

        assertThat(service.resolve("abc123")).isEqualTo("https://example.com");
    }

    @Test
    void resolve_notFound_throwsShortCodeNotFound() {
        when(repository.findByShortCodeAndDeletedFalse("xyz"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("xyz"))
            .isInstanceOf(ShortCodeNotFoundException.class);
    }

    @Test
    void resolve_expiredUrl_throwsUrlExpired() {
        ShortenedUrl url = ShortenedUrl.builder()
            .id(1L).shortCode("abc123").originalUrl("https://example.com")
            .createdAt(LocalDateTime.now().minusDays(40))
            .expiresAt(LocalDateTime.now().minusDays(5)) // expired 5 days ago
            .build();

        when(repository.findByShortCodeAndDeletedFalse("abc123"))
            .thenReturn(Optional.of(url));

        assertThatThrownBy(() -> service.resolve("abc123"))
            .isInstanceOf(UrlExpiredException.class);
    }

    @Test
    void delete_existingUrl_marksDeleted() {
        ShortenedUrl url = ShortenedUrl.builder()
            .id(1L).shortCode("abc123").originalUrl("https://example.com")
            .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(30))
            .deleted(false)
            .build();

        when(repository.findByShortCodeAndDeletedFalse("abc123"))
            .thenReturn(Optional.of(url));
        when(repository.save(any())).thenReturn(url);

        service.delete("abc123");

        assertThat(url.isDeleted()).isTrue();
        verify(repository).save(url);
    }

    @Test
    void delete_notFound_throwsShortCodeNotFound() {
        when(repository.findByShortCodeAndDeletedFalse("nope"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("nope"))
            .isInstanceOf(ShortCodeNotFoundException.class);
    }
}
