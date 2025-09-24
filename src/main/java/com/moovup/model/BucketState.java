package com.moovup.model;

/**
 *
 * @author thiyagaraja
 */
public class BucketState {
    private final double capacity;
        private final double leakRate;
        private double currentLevel;
        private double lastLeakTime;
        
        public BucketState(double capacity, double leakRate, double timestamp) {
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.currentLevel = 0.0;
            this.lastLeakTime = timestamp;
        }
        
        public void leak(double currentTime) {
            if (currentTime < lastLeakTime) {
                return;
            }
            
            double timePassed = currentTime - lastLeakTime;
            double leakAmount = timePassed * leakRate;
            currentLevel = Math.max(0.0, currentLevel - leakAmount);
            lastLeakTime = currentTime;
        }
        
        public boolean allowRequest(double requestSize) {
            if (currentLevel + requestSize > capacity) {
                return false;
            }
            currentLevel += requestSize;
            return true;
        }
        public double getCurrentLevel() { return currentLevel; }
        public double getCapacity() { return capacity; }
        public double getLeakRate() { return leakRate; }
        public double getLastLeakTime() { return lastLeakTime; }
}
