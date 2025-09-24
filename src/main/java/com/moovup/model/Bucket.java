
package com.moovup.model;

/**
 *
 * @author thiyagaraja
 */
public class Bucket {
    private final String userId;
        private final double currentLevel;
        private final double capacity;
        private final double leakRate;
        private final double lastLeakTime;
        
        public Bucket(String userId, double currentLevel, double capacity, 
                         double leakRate, double lastLeakTime) {
            this.userId = userId;
            this.currentLevel = currentLevel;
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.lastLeakTime = lastLeakTime;
        }
        public String getUserId() { return userId; }
        public double getCurrentLevel() { return currentLevel; }
        public double getCapacity() { return capacity; }
        public double getLeakRate() { return leakRate; }
        public double getLastLeakTime() { return lastLeakTime; }
        
        @Override
        public String toString() {
            return String.format("Bucket{userId='%s', level=%.2f/%.2f, leakRate=%.2f, lastLeak=%.2f}",
                    userId, currentLevel, capacity, leakRate, lastLeakTime);
        }
}
