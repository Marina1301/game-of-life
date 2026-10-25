package org.example;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class FoodResource {
    private final Semaphore food;

    public FoodResource(int initialFood) {
        this.food = new Semaphore(Math.max(0, initialFood), true);
    }

    public boolean tryConsume(long timeoutMs) throws InterruptedException {
        return food.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public void addFood(int amount) {
        if (amount > 0) {
            food.release(amount);
        }
    }

    public int getAvailableFood() {
        return food.availablePermits();
    }
}
