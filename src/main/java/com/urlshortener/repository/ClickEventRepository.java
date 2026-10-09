package com.urlshortener.repository;

import com.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortCode(String shortCode);

    List<ClickEvent> findTop20ByShortCodeOrderByClickedAtDesc(String shortCode);

    @Query("SELECT c.referrer, COUNT(c) as cnt FROM ClickEvent c " +
           "WHERE c.shortCode = :shortCode AND c.referrer IS NOT NULL " +
           "GROUP BY c.referrer ORDER BY cnt DESC")
    List<Object[]> findTopReferrersByShortCode(@Param("shortCode") String shortCode);
}
