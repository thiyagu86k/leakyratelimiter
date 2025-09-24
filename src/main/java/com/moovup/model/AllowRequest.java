package com.moovup.model;

import com.moovup.service.LeakyBucketRateLimiter;


/**
 *
 * @author thiyagaraja
 */
public class AllowRequest {
    private final boolean allowed;
        private final LeakyBucketRateLimiter newLimiterState;
        
        public AllowRequest(boolean allowed, LeakyBucketRateLimiter newLimiterState) {
            this.allowed = allowed;
            this.newLimiterState = newLimiterState;
        }
        
        public boolean isAllowed() { return allowed; }
        public LeakyBucketRateLimiter getNewLimiterState() { return newLimiterState; }
    
}
