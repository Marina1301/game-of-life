package org.example.faulty.race;

class RaceFoodResource {
    private int availableFood;
    private int lowestObserved;

    RaceFoodResource(int initialFood) {
        this.availableFood = initialFood;
        this.lowestObserved = initialFood;
    }

    boolean tryConsume() {
        if (availableFood > 0) {
            int current = availableFood;
            availableFood = current - 1;
            if (availableFood < lowestObserved) {
                lowestObserved = availableFood;
            }
            return true;
        }
        return false;
    }

    void addFood(int amount) {
        int current = availableFood;
        availableFood = current + amount;
    }

    int getAvailableFood() {
        return availableFood;
    }

    int getLowestObserved() {
        return lowestObserved;
    }
}
