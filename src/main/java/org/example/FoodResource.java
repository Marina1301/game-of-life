package org.example;

import java.util.concurrent.locks.ReentrantLock;

public class FoodResource {
    private final ReentrantLock lock = new ReentrantLock();
    private int availableFood;

    public FoodResource(int initialFood) {
        this.availableFood = Math.max(0, initialFood);
    }

    public boolean consumeOne() {
        lock.lock();
        try {
            if (availableFood <= 0) {
                return false;
            }

            availableFood--;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void addFood(int amount) {
        if (amount <= 0) {
            return;
        }

        lock.lock();
        try {
            availableFood += amount;
        } finally {
            lock.unlock();
        }
    }

    public int getAvailableFood() {
        lock.lock();
        try {
            return availableFood;
        } finally {
            lock.unlock();
        }
    }
}
