package com.example.accountservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ResilienceConfig {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker cb = entryAddedEvent.getAddedEntry();
                log.info("[CIRCUIT-BREAKER] Registered CircuitBreaker: {}", cb.getName());
                cb.getEventPublisher()
                        .onStateTransition(event -> log.info("[CIRCUIT-BREAKER] [{}] State transition: {} -> {}",
                                event.getCircuitBreakerName(),
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                        .onError(event -> log.warn("[CIRCUIT-BREAKER] [{}] Call failed: {} (duration: {}ms)",
                                event.getCircuitBreakerName(),
                                event.getThrowable() != null ? event.getThrowable().getClass().getSimpleName() : "Unknown",
                                event.getElapsedDuration().toMillis()))
                        .onSuccess(event -> log.debug("[CIRCUIT-BREAKER] [{}] Call succeeded (duration: {}ms)",
                                event.getCircuitBreakerName(),
                                event.getElapsedDuration().toMillis()))
                        .onCallNotPermitted(event -> log.warn("[CIRCUIT-BREAKER] [{}] Call NOT permitted (circuit is OPEN)",
                                event.getCircuitBreakerName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
            }
        };
    }

    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                Retry retry = entryAddedEvent.getAddedEntry();
                log.info("[RETRY] Registered Retry: {}", retry.getName());
                retry.getEventPublisher()
                        .onRetry(event -> log.warn("[RETRY] [{}] Retry attempt #{} after error: {}",
                                event.getName(),
                                event.getNumberOfRetryAttempts(),
                                event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "Unknown"))
                        .onSuccess(event -> log.debug("[RETRY] [{}] Retry call succeeded after attempts",
                                event.getName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
            }
        };
    }

    @Bean
    public ApplicationRunner initializeResilienceBeans(CircuitBreakerRegistry cbRegistry, RetryRegistry retryRegistry) {
        return args -> {
            CircuitBreaker cb = cbRegistry.circuitBreaker("userServiceCircuitBreaker");
            Retry retry = retryRegistry.retry("userServiceRetry");
            log.info("[RESILIENCE-INIT] Initialized CircuitBreaker [{}] (state: {}) and Retry [{}]",
                    cb.getName(), cb.getState(), retry.getName());
        };
    }
}
