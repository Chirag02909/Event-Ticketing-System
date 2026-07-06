package com.ticketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables @Async method execution across the application.
 *
 * Without @EnableAsync, the @Async annotation on EmailServiceImpl methods
 * is silently ignored — Spring runs them synchronously, defeating the
 * entire purpose (this is a very common "why isn't this actually async?" bug).
 *
 * THREAD POOL SIZING:
 * The pool size values come from application.properties:
 *   spring.task.execution.pool.core-size=5
 *   spring.task.execution.pool.max-size=20
 *   spring.task.execution.pool.queue-capacity=100
 *
 * These were added back in Chunk 1's application.properties — Spring Boot
 * auto-configures a TaskExecutor bean from those properties automatically.
 * We define our own bean here ONLY to give it a custom thread name prefix,
 * which makes background threads identifiable in logs and thread dumps
 * (e.g. "ticketing-async-1" instead of a generic "task-1").
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ticketing-async-");
        executor.initialize();
        return executor;
    }
}