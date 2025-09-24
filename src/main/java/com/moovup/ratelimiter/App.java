package com.moovup.ratelimiter;


import com.moovup.model.AllowRequest;
import com.moovup.model.Bucket;
import com.moovup.service.LeakyBucketRateLimiter;

/**
 *
 * @author thiyagaraja
 */
public class App {

      public static void main(String[] args) {
        System.out.println("Leaky Bucket Rate Limiter");
        System.out.println("=========================\n");
        
        // Create rate limiter: capacity=3, leak_rate=1.0/sec
        LeakyBucketRateLimiter limiter = 
                LeakyBucketRateLimiter.createRateLimiter(3.0, 1.0);
        
        System.out.println("Configuration:");
        System.out.println("- Bucket Capacity: " + limiter.getCapacity());
        System.out.println("- Leak Rate: " + limiter.getLeakRate() + " units/second\n");
       
        basicUsageRateLimiter(limiter);
        burstHandlingRateLimiter(limiter);
        timeBasedLeakingRateLimiter(limiter);
        multipleUsers(limiter);
    }
    
    private static void basicUsageRateLimiter(LeakyBucketRateLimiter limiter) {
        System.out.println("Basic Usage");
        System.out.println("============");
        
        LeakyBucketRateLimiter currentLimiter = limiter;
        
        for (int i = 1; i <= 4; i++) {
           
            AllowRequest result = 
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "raja", 0.0);
            
            System.out.printf("Request %d: %s\n", i, 
                    result.isAllowed() ? "ALLOWED" : "REJECTED");
            
            if (result.isAllowed()) {
                currentLimiter = result.getNewLimiterState();
                Bucket info = 
                        LeakyBucketRateLimiter.getBucketState(currentLimiter, "raja");
                System.out.printf("   Bucket level: %.1f/%.1f\n", 
                        info.getCurrentLevel(), info.getCapacity());
            }
        }
        System.out.println();
    }
    
    private static void burstHandlingRateLimiter(LeakyBucketRateLimiter limiter) {
        System.out.println("Burst Handling");
        System.out.println("==============");
        
        LeakyBucketRateLimiter currentLimiter = limiter;
        
        System.out.println("Sending 5 rapid requests at time=0:");
        for (int i = 1; i <= 5; i++) {
            AllowRequest result = 
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "nitheesh", 0.0);
            
            System.out.printf("Request %d: %s", i, 
                    result.isAllowed() ? "ALLOWED" : "REJECTED");
            
            if (result.isAllowed()) {
                currentLimiter = result.getNewLimiterState();
                Bucket info = 
                        LeakyBucketRateLimiter.getBucketState(currentLimiter, "nitheesh");
                System.out.printf(" (level: %.1f)", info.getCurrentLevel());
            }
            System.out.println();
        }
        System.out.println();
    }
    
    private static void timeBasedLeakingRateLimiter(LeakyBucketRateLimiter limiter) {
        System.out.println("Time based Leaking");
        System.out.println("==================");
        
        LeakyBucketRateLimiter currentLimiter = limiter;
        
        System.out.println("Filling bucket at time=0:");
        for (int i = 1; i <= 3; i++) {
            AllowRequest result = 
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "agila", 0.0);
            currentLimiter = result.getNewLimiterState();
        }
        
       Bucket info = 
                LeakyBucketRateLimiter.getBucketState(currentLimiter, "agila");
        System.out.printf("Bucket full: %.1f/%.1f\n\n", 
                info.getCurrentLevel(), info.getCapacity());
        
        double[] testTimes = {1.0, 2.0, 5.0};
        for (double time : testTimes) {
            AllowRequest result = 
                    LeakyBucketRateLimiter.allowRequest(currentLimiter, "agila", time);
            
            System.out.printf("Request at time=%.1f: %s\n", time, 
                    result.isAllowed() ? "ALLOWED" : "REJECTED");
            
            if (result.isAllowed()) {
                currentLimiter = result.getNewLimiterState();
                info = LeakyBucketRateLimiter.getBucketState(currentLimiter, "agila");
                System.out.printf("   Bucket level: %.1f (leaked %.1f units)\n", 
                        info.getCurrentLevel(), 3.0 + 1.0 - info.getCurrentLevel());
            }
        }
        System.out.println();
    }
    
    private static void multipleUsers(LeakyBucketRateLimiter limiter) {
        System.out.println("Multiple Users");
        System.out.println("==============");
        
        LeakyBucketRateLimiter currentLimiter = limiter;
        String[] users = {"raja", "nitheesh", "agila"};
        for (String user : users) {
            System.out.printf("User '%s' requests:\n", user);
            for (int i = 1; i <= 2; i++) {
                AllowRequest result = 
                        LeakyBucketRateLimiter.allowRequest(currentLimiter, user, 0.0);
                
                System.out.printf("  Request %d: %s", i, 
                        result.isAllowed() ? "ALLOWED" : "REJECTED");
                
                if (result.isAllowed()) {
                    currentLimiter = result.getNewLimiterState();
                    Bucket info = 
                            LeakyBucketRateLimiter.getBucketState(currentLimiter, user);
                    System.out.printf(" (level: %.1f)", info.getCurrentLevel());
                }
                System.out.println();
            }
        }
        
        System.out.println("\nFinal bucket states:");
        LeakyBucketRateLimiter.getAllBucketStates(currentLimiter)
                .forEach((userId, bucketInfo) -> 
                        System.out.printf("  %s: %.1f/%.1f\n", 
                                userId, bucketInfo.getCurrentLevel(), bucketInfo.getCapacity()));
    }
}
