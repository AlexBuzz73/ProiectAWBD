package com.example.gatewayservice.ratelimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int capacity, double refillTokensPerSecond) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillTokensPerSecond));
        return bucket.tryConsume();
    }

    public void reset() {
        buckets.clear();
    }

    private static class TokenBucket {
        private final int capacity;
        private final double refillTokensPerSecond;
        private double availableTokens;
        private long lastRefillNanos;

        public TokenBucket(int capacity, double refillTokensPerSecond) {
            this.capacity = capacity;
            this.refillTokensPerSecond = refillTokensPerSecond;
            this.availableTokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (availableTokens >= 1.0) {
                availableTokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double secondsPassed = (now - lastRefillNanos) / 1_000_000_000.0;
            if (secondsPassed > 0) {
                double tokensToAdd = secondsPassed * refillTokensPerSecond;
                availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
                lastRefillNanos = now;
            }
        }
    }
}
