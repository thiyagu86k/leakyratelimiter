package com.moovup.service;

import com.moovup.model.AllowRequest;
import com.moovup.model.Bucket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

public class LeakyBucketRateLimiterTest {

    private LeakyBucketRateLimiter limiter;
    private static final double CAPACITY = 5.0;
    private static final double LEAK_RATE = 1.0;
    private static final double DELTA = 0.001;

    @BeforeEach
    void setUp() {
        limiter = LeakyBucketRateLimiter.createRateLimiter(CAPACITY, LEAK_RATE);
    }

    @Test
    void testCreateRateLimiter() {
        LeakyBucketRateLimiter rl = LeakyBucketRateLimiter.createRateLimiter(10, 2.0);
        assertEquals(10.0, rl.getCapacity(), DELTA);
        assertEquals(2.0, rl.getLeakRate(), DELTA);
        assertEquals(0, rl.getUserCount());

        assertThrows(IllegalArgumentException.class,
                () -> LeakyBucketRateLimiter.createRateLimiter(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> LeakyBucketRateLimiter.createRateLimiter(-1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> LeakyBucketRateLimiter.createRateLimiter(5, -1));
    }

    @Test
    void testBasicFunctionality() {
        AllowRequest result1 =
                LeakyBucketRateLimiter.allowRequest(limiter, "user1", 0.0);
        assertTrue(result1.isAllowed());
        AllowRequest result2 =
                LeakyBucketRateLimiter.allowRequest(result1.getNewLimiterState(), "user1", 1.0);
        assertTrue(result2.isAllowed());
        Bucket bucket =
                LeakyBucketRateLimiter.getBucketState(result2.getNewLimiterState(), "user1");
        assertNotNull(bucket);
        assertEquals("user1", bucket.getUserId());
        assertEquals(1.0, bucket.getCurrentLevel(), DELTA);
    }

    @Test
    void testBurstHandling() {
        LeakyBucketRateLimiter currentLimiter = limiter;
        for (int i = 0; i < 5; i++) {
            AllowRequest result =
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
            currentLimiter = result.getNewLimiterState();
        }
        AllowRequest overflowResult =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
        assertFalse(overflowResult.isAllowed());
        Bucket bucket =
                LeakyBucketRateLimiter.getBucketState(currentLimiter, "user1");
        assertEquals(5.0, bucket.getCurrentLevel(), DELTA);
    }

    @Test
    void testTimeBasedLeaking() {
        LeakyBucketRateLimiter currentLimiter = limiter;
        for (int i = 0; i < 5; i++) {
            AllowRequest result =
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
            assertTrue(result.isAllowed());
            currentLimiter = result.getNewLimiterState();
        }
        AllowRequest result =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 2.0);
        assertTrue(result.isAllowed(), "Request should be allowed after leaking");
        Bucket bucket =
                LeakyBucketRateLimiter.getBucketState(result.getNewLimiterState(), "user1");
        assertEquals(4.0, bucket.getCurrentLevel(), DELTA);
        result = LeakyBucketRateLimiter.allowRequest(result.getNewLimiterState(), "user1", 10.0);
        assertTrue(result.isAllowed());

        bucket = LeakyBucketRateLimiter.getBucketState(result.getNewLimiterState(), "user1");
        assertEquals(1.0, bucket.getCurrentLevel(), DELTA);
    }

    @Test
    void testMultipleUsers() {
        LeakyBucketRateLimiter currentLimiter = limiter;
        for (int i = 0; i < 5; i++) {
            AllowRequest result =
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
            assertTrue(result.isAllowed());
            currentLimiter = result.getNewLimiterState();
        }
        AllowRequest user1Result =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
        assertFalse(user1Result.isAllowed());

        AllowRequest user2Result =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user2", 0.0);
        assertTrue(user2Result.isAllowed());

        Map<String, Bucket> allStates =
                LeakyBucketRateLimiter.getAllBucketStates(user2Result.getNewLimiterState());

        assertEquals(2, allStates.size());
        assertEquals(5.0, allStates.get("user1").getCurrentLevel(), DELTA);
        assertEquals(1.0, allStates.get("user2").getCurrentLevel(), DELTA);
    }

    @Test
    void testFirstRequestFromNewUser() {
        AllowRequest result =
                LeakyBucketRateLimiter.allowRequest(limiter, "newUser", 100.0);
        assertTrue(result.isAllowed());

        Bucket bucket =
                LeakyBucketRateLimiter.getBucketState(result.getNewLimiterState(), "newUser");
        assertNotNull(bucket);
        assertEquals(1.0, bucket.getCurrentLevel(), DELTA);
        assertEquals(100.0, bucket.getLastLeakTime(), DELTA);
    }

    @Test
    void testBackwardsTimestamps() {
        LeakyBucketRateLimiter currentLimiter = limiter;
        AllowRequest result1 =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 10.0);
        assertTrue(result1.isAllowed());
        AllowRequest result2 =
                LeakyBucketRateLimiter.allowRequest(result1.getNewLimiterState(), "user1", 5.0);
        assertTrue(result2.isAllowed());
        Bucket info =
                LeakyBucketRateLimiter.getBucketState(result2.getNewLimiterState(), "user1");
        assertEquals(2.0, info.getCurrentLevel(), DELTA);
        assertEquals(10.0, info.getLastLeakTime(), DELTA);
    }

    @Test
    void testVeryLargeTimeGaps() {
        LeakyBucketRateLimiter currentLimiter = limiter;
        for (int i = 0; i < 5; i++) {
            AllowRequest result =
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
            assertTrue(result.isAllowed());
            currentLimiter = result.getNewLimiterState();
        }
        AllowRequest result =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 1000.0);
        assertTrue(result.isAllowed());
        Bucket bucket =
                LeakyBucketRateLimiter.getBucketState(result.getNewLimiterState(), "user1");
        assertEquals(1.0, bucket.getCurrentLevel(), DELTA);
    }

    @Test
    void testBucketOverflowScenarios() {
        LeakyBucketRateLimiter currentLimiter = limiter;

        for (int i = 0; i < 5; i++) {
           AllowRequest result =
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
            assertTrue(result.isAllowed());
            currentLimiter = result.getNewLimiterState();
        }
        AllowRequest overflowResult =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
        assertFalse(overflowResult.isAllowed());
        AllowRequest partialLeakResult =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.5);
        assertFalse(partialLeakResult.isAllowed());
        Bucket bucket =
                LeakyBucketRateLimiter.getBucketState(partialLeakResult.getNewLimiterState(), "user1");
        assertEquals(4.5, bucket.getCurrentLevel(), DELTA);
    }

    @Test
    void testGetBucketStateEdgeCases() {
        assertNull(LeakyBucketRateLimiter.getBucketState(null, "user1"));
        assertNull(LeakyBucketRateLimiter.getBucketState(limiter, null));
        assertNull(LeakyBucketRateLimiter.getBucketState(limiter, "nonexistent"));
        assertNull(LeakyBucketRateLimiter.getBucketState(limiter, ""));
    }

    @Test
    void testInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> LeakyBucketRateLimiter.allowRequest(null, "user1", 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> LeakyBucketRateLimiter.allowRequest(limiter, null, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> LeakyBucketRateLimiter.allowRequest(limiter, "", 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> LeakyBucketRateLimiter.allowRequest(limiter, "user1", 0.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> LeakyBucketRateLimiter.allowRequest(limiter, "user1", 0.0, -1.0));
    }

    @Test
    void testCustomRequestSizes() {
      AllowRequest result =
                LeakyBucketRateLimiter.allowRequest(limiter, "user1", 0.0, 10.0);
        assertFalse(result.isAllowed());
        result = LeakyBucketRateLimiter.allowRequest(limiter, "user1", 0.0, 0.5);
        assertTrue(result.isAllowed());
        Bucket bucket =
                LeakyBucketRateLimiter.getBucketState(result.getNewLimiterState(), "user1");
        assertEquals(0.5, bucket.getCurrentLevel(), DELTA);
    }

    @Test
    void testZeroLeakRate() {
        LeakyBucketRateLimiter zeroLeakLimiter =
                LeakyBucketRateLimiter.createRateLimiter(3.0, 0.0);

        LeakyBucketRateLimiter currentLimiter = zeroLeakLimiter;

        for (int i = 0; i < 3; i++) {
            AllowRequest result =
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 0.0);
            assertTrue(result.isAllowed());
            currentLimiter = result.getNewLimiterState();
        }
        AllowRequest result =
                LeakyBucketRateLimiter.allowRequest(currentLimiter, "user1", 100.0);
        assertFalse(result.isAllowed());

        Bucket bucket =
                LeakyBucketRateLimiter.getBucketState(currentLimiter, "user1");
        assertEquals(3.0, bucket.getCurrentLevel(), DELTA);
    }
}