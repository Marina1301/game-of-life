package org.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Cell implements Runnable {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Simulation simulation;
    private final int id;
    private final CellType type;
    private final long starvationDurationMs;
    private final Random random = new Random();
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final AtomicBoolean reproducing = new AtomicBoolean(false);

    private int feedCount;
    private long lastMealTime;

    public Cell(Simulation simulation, int id, CellType type, long starvationDurationMs) {
        this.simulation = simulation;
        this.id = id;
        this.type = type;
        this.starvationDurationMs = starvationDurationMs;
        this.lastMealTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (alive.get() && simulation.isRunning()) {
            int beforeFood = simulation.getFoodAvailable();
            if (simulation.consumeFood()) {
                int afterFood = simulation.getFoodAvailable();
                feedCount++;
                lastMealTime = System.currentTimeMillis();
                System.out.printf("[%s] Cell %d (%s) consumed food: %d -> %d%n", timestamp(), id, type, beforeFood, afterFood);

                if (feedCount >= 10) {
                    attemptReproduction();
                    feedCount = 0;
                }

                sleepRandomly();
            } else {
                long elapsed = System.currentTimeMillis() - lastMealTime;
                if (elapsed >= starvationDurationMs) {
                    int foodProduced = generateFoodOnStarvation();
                    die(foodProduced);
                    System.out.printf("[%s] Cell %d starved and produced %d food units.%n", timestamp(), id, foodProduced);
                    break;
                }

                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void attemptReproduction() {
        if (type == CellType.ASEXUAL) {
            simulation.reproduceAsexual(this);
        } else {
            simulation.reproduceSexual(this);
        }
    }

    public boolean tryBeginReproduction() {
        return reproducing.compareAndSet(false, true);
    }

    public void finishReproduction() {
        reproducing.set(false);
    }

    public boolean isAlive() {
        return alive.get();
    }

    public int getId() {
        return id;
    }

    public CellType getType() {
        return type;
    }

    private void sleepRandomly() {
        try {
            TimeUnit.MILLISECONDS.sleep(150L + random.nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void die(int foodAmount) {
        alive.set(false);
        simulation.addFood(foodAmount);
    }

    int generateFoodOnStarvation() {
        return 1 + random.nextInt(5);
    }

    private static String timestamp() {
        return LocalTime.now().format(TIME_FORMATTER);
    }

    @Override
    public String toString() {
        return "Cell-" + id + "(" + type + ")";
    }
}
