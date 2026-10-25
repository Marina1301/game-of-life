package org.example.faulty.deadlock;

import org.example.CellType;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

class DeadlockCell implements Runnable {
    private static final int MEALS_TO_REPRODUCE = 10;

    private final DeadlockSimulation simulation;
    private final int id;
    private final CellType type;
    private final long fullDurationMs;
    private final long starvationDurationMs;
    private final AtomicBoolean alive = new AtomicBoolean(true);

    private int feedCount;

    DeadlockCell(DeadlockSimulation simulation, int id, CellType type, long fullDurationMs, long starvationDurationMs) {
        this.simulation = simulation;
        this.id = id;
        this.type = type;
        this.fullDurationMs = fullDurationMs;
        this.starvationDurationMs = starvationDurationMs;
    }

    @Override
    public void run() {
        try {
            while (alive.get() && simulation.isRunning()) {
                if (!simulation.tryConsumeFood(starvationDurationMs)) {
                    if (simulation.isRunning()) {
                        starve();
                    }
                    break;
                }

                feedCount++;
                simulation.recordMeal();
                long fullUntil = System.currentTimeMillis() + fullDurationMs;

                if (feedCount >= MEALS_TO_REPRODUCE) {
                    boolean reproduced = (type == CellType.ASEXUAL)
                            ? simulation.divide(this)
                            : simulation.mate(this, fullUntil - System.currentTimeMillis());
                    if (reproduced) {
                        feedCount = 0;
                    }
                    if (!alive.get()) {
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
            simulation.onCellFinished(this);
        }
    }

    private void starve() {
        if (alive.compareAndSet(true, false)) {
            simulation.addFood(1 + ThreadLocalRandom.current().nextInt(5));
        }
    }

    boolean deactivate() {
        return alive.compareAndSet(true, false);
    }

    int getId() {
        return id;
    }

    CellType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "DeadlockCell-" + id + "(" + type + ")";
    }
}
