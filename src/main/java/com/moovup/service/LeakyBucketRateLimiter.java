package com.moovup.service;

import com.moovup.model.AllowRequest;
import com.moovup.model.Bucket;
import com.moovup.model.BucketState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author thiyagaraja
 */
public class LeakyBucketRateLimiter {
    private final double capacity;
    private final double leakRate;
    private final Map<String, BucketState> userBuckets;
    
  
    private LeakyBucketRateLimiter(double capacity, double leakRate, 
                                  Map<String, BucketState> userBuckets) {
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.userBuckets = new ConcurrentHashMap<>(userBuckets);
    }
    
    public static LeakyBucketRateLimiter createRateLimiter(double capacity, double leakRate) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (leakRate < 0) {
            throw new IllegalArgumentException("Leak rate cannot be negative");
        }
        
        return new LeakyBucketRateLimiter(capacity, leakRate, new ConcurrentHashMap<>());
    }
    
    public static AllowRequest allowRequest(LeakyBucketRateLimiter limiter, 
                                                 String userId, double timestamp) {
        return allowRequest(limiter, userId, timestamp, 1.0);
    }
    
    public static AllowRequest allowRequest(LeakyBucketRateLimiter limiter, 
                                                 String userId, double timestamp, 
                                                 double requestSize) {
        if (limiter == null) {
            throw new IllegalArgumentException("Limiter cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (requestSize <= 0) {
            throw new IllegalArgumentException("Request size must be positive");
        }
        Map<String, BucketState> newBuckets = new ConcurrentHashMap<>(limiter.userBuckets);
        BucketState bucket = newBuckets.get(userId);
        if (bucket == null) {
            bucket = new BucketState(limiter.capacity, limiter.leakRate, timestamp);
            newBuckets.put(userId, bucket);
        }
        bucket.leak(timestamp);
        boolean allowed = bucket.allowRequest(requestSize);
        LeakyBucketRateLimiter newLimiter = new LeakyBucketRateLimiter(
                limiter.capacity, limiter.leakRate, newBuckets);
        return new AllowRequest(allowed, newLimiter);
    }
    
    public static Bucket getBucketState(LeakyBucketRateLimiter limiter, String userId) {
        if (limiter == null || userId == null) {
            return null;
        }
        
        BucketState bucket = limiter.userBuckets.get(userId);
        if (bucket == null) {
            return null;
        }
        
        return new Bucket(
                userId,
                bucket.getCurrentLevel(),
                bucket.getCapacity(),
                bucket.getLeakRate(),
                bucket.getLastLeakTime()
        );
    }
    
    public static Map<String, Bucket> getAllBucketStates(LeakyBucketRateLimiter limiter) {
        if (limiter == null) {
            return new ConcurrentHashMap<>();
        }
        
        Map<String, Bucket> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, BucketState> entry : limiter.userBuckets.entrySet()) {
            String userId = entry.getKey();
            BucketState bucket = entry.getValue();
            result.put(userId, new Bucket(
                    userId,
                    bucket.getCurrentLevel(),
                    bucket.getCapacity(),
                    bucket.getLeakRate(),
                    bucket.getLastLeakTime()
            ));
        }
        return result;
    }
    
    public double getCapacity() { return capacity; }
    public double getLeakRate() { return leakRate; }
    public int getUserCount() { return userBuckets.size(); }
    
}
