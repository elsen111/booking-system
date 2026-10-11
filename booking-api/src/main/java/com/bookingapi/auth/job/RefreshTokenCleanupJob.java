package com.bookingapi.auth.job;

import com.bookingapi.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository repository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpired() {
        int deleted = repository.deleteExpired(Instant.now());
        log.info("Deleted {} expired refresh tokens", deleted);
    }
}