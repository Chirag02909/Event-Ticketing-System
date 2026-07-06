package com.ticketing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled annotation processing.
 *
 * Without this, @Scheduled on SeatHoldExpiryScheduler is silently ignored —
 * the method exists but never runs. This is a common "why isn't my scheduler
 * running?" bug.
 *
 * @EnableScheduling is intentionally in its own config class rather than on
 * the main application class, so scheduling can be disabled in tests by simply
 * not loading this config.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}