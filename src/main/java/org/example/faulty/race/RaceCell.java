package org.example.faulty.race;

import org.example.CellType;

import java.util.concurrent.ThreadLocalRandom;


class RaceCell implements Runnable {
    static final int MEALS_TO_REPRODUCE = 10;

    private final RaceSimulation simulation;
    final int id;
    final CellType type;
    private final long fullDurationMs;
    private final long starvationDurationMs;

    boolean alive = true;
    boolean paired;

    private int feedCount;

    RaceCell(RaceSimulation simulation, int id, CellType type, long fullDurationMs, long starvationDurationMs) {
        this.simulation = simulation;
        this.id = id;
        this.type = type;
        this.fullDurationMs = fullDurationMs;
        this.starvationDurationMs = starvationDurationMs;
    }

    @Override
    public void run() {
        simulation.cellStarted();
        try {
            while (alive && simulation.isRunning()) {
                if (!eatWithin(starvationDurationMs)) {
                    if (simulation.isRunning()) {
                        starve();
                    }
                    break;
                }

                feedCount++;
                long fullUntil = System.currentTimeMillis() + fullDurationMs;

                if (feedCount >= MEALS_TO_REPRODUCE) {
                    boolean reproduced = (type == CellType.ASEXUAL)
                            ? simulation.divide(this)
                            : simulation.mate(this, fullUntil - System.currentTimeMillis());
                    if (reproduced) {
                        feedCount = 0;
                    }
                    if (!alive) {
                        break;
                    }
                }

                long remaining = fullUntil - System.currentTimeMillis();
                if (remaining > 0) {
                    Thread.sleep(remaining);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            simulation.cellFinished();
        }
    }

    private boolean eatWithin(long windowMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + windowMs;
        while (simulation.isRunning()) {
            if (simulation.food.tryConsume()) {
                simulation.recordMeal();
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(2);
        }
        return false;
    }

    private void starve() {
        alive = false;
        int produced = 1 + ThreadLocalRandom.current().nextInt(5);
        simulation.food.addFood(produced);
        simulation.recordProduced(produced);
    }

    @Override
    public String toString() {
        return "RaceCell-" + id + "(" + type + ")";
    }
}
