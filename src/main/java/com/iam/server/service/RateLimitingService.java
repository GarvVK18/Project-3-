package com.iam.server.service;

import java.time.Duration;

public interface RateLimitingService {

    boolean isAllowed(String key, int maxRequests, Duration window);

    long getRemainingRequests(String key, int maxRequests);

    long getResetSeconds(String key, Duration window);
}
