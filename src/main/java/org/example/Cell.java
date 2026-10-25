package org.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class Cell implements Runnable {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MEALS_TO_REPRODUCE = 10;

    private final Simulation simulation;
    private final int id;
    private final CellType type;
    private final long fullDurationMs;       // T_full
    private final long starvationDurationMs; // T_starve

    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final AtomicBoolean paired = new AtomicBoolean(false);

    private int feedCount;

    public Cell(Simulation simulation, int id, CellType type, long fullDurationMs, long starvationDurationMs) {
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
                log("ate (meal #%d). Food left: %d", feedCount, simulation.getFoodAvailable());

                long fullUntil = System.currentTimeMillis() + fullDurationMs;

                if (feedCount >= MEALS_TO_REPRODUCE) {
                    if (reproduce(fullUntil)) {
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

    private boolean reproduce(long fullUntil) throws InterruptedException {
        if (type == CellType.ASEXUAL) {
            return simulation.divide(this);
        }
        return simulation.mate(this, fullUntil - System.currentTimeMillis());
    }

    private void starve() {
        if (alive.compareAndSet(true, false)) {
            int foodProduced = 1 + ThreadLocalRandom.current().nextInt(5);
            simulation.addFood(foodProduced);
            log("starved to death and produced %d food units.", foodProduced);
        }
    }

    boolean deactivate() {
        return alive.compareAndSet(true, false);
    }

    void markPaired() {
        paired.set(true);
    }

    void resetPaired() {
        paired.set(false);
    }

    boolean isPaired() {
        return paired.get();
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

    private void log(String format, Object... args) {
        String prefix = String.format("[%s] Cell %d (%s) ", LocalTime.now().format(TIME_FORMATTER), id, type);
        System.out.println(prefix + String.format(format, args));
    }

    @Override
    public String toString() {
        return "Cell-" + id + "(" + type + ")";
    }
}
