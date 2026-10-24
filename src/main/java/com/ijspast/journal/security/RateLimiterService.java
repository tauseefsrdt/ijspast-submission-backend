package com.ijspast.journal.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, RequestTracker> trackers = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private static final long TIME_WINDOW_MS = 60000; // 1 minute

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        trackers.compute(key, (k, tracker) -> {
            if (tracker == null || now - tracker.windowStartTime > TIME_WINDOW_MS) {
                return new RequestTracker(now, 1);
            }
            tracker.count++;
            return tracker;
        });

        RequestTracker tracker = trackers.get(key);
        return tracker != null && tracker.count <= MAX_REQUESTS_PER_MINUTE;
    }

    private static class RequestTracker {
        final long windowStartTime;
        int count;

        RequestTracker(long windowStartTime, int count) {
            this.windowStartTime = windowStartTime;
            this.count = count;
        }
    }
}
