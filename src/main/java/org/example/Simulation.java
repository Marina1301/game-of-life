package org.example;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Simulation {
    private final FoodResource foodResource;
    private final CopyOnWriteArrayList<Cell> cells = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final ReentrantLock reproductionLock = new ReentrantLock();
    private final long fullDurationMs;
    private final long starvationDurationMs;

    public Simulation(int cellCount, int initialFood, long fullDurationMs, long starvationDurationMs) {
        this.foodResource = new FoodResource(initialFood);
        this.fullDurationMs = fullDurationMs;
        this.starvationDurationMs = starvationDurationMs;

        for (int i = 0; i < cellCount; i++) {
            CellType type = (i % 2 == 0) ? CellType.ASEXUAL : CellType.SEXUAL;
            Cell cell = new Cell(this, idGenerator.getAndIncrement(), type, fullDurationMs, starvationDurationMs);
            cells.add(cell);
        }
    }

    public void start() {
        if (running.getAndSet(true)) {
            return;
        }

        for (Cell cell : cells) {
            Thread thread = new Thread(cell, "Cell-" + cell.getId());
            thread.start();
        }

        System.out.println("Simulation started with " + cells.size() + " cells.");
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean consumeFood() {
        return foodResource.consumeOne();
    }

    public void addFood(int amount) {
        foodResource.addFood(amount);
    }

    public void reproduceAsexual(Cell parent) {
        if (!running.get()) {
            return;
        }

        Cell child = new Cell(this, idGenerator.getAndIncrement(), parent.getType(), fullDurationMs, starvationDurationMs);
        cells.add(child);
        new Thread(child, "Cell-" + child.getId()).start();

        System.out.println("Asexual reproduction: " + parent + " created " + child);
    }

    public void reproduceSexual(Cell parent) {
        if (!parent.tryBeginReproduction()) {
            return;
        }

        reproductionLock.lock();
        try {
            Cell partner = null;

            for (Cell candidate : cells) {
                if (candidate == parent || !candidate.isAlive() || candidate.getType() != CellType.SEXUAL) {
                    continue;
                }

                if (candidate.tryBeginReproduction()) {
                    partner = candidate;
                    break;
                }
            }

            if (partner == null) {
                parent.finishReproduction();
                return;
            }

            Cell child = new Cell(this, idGenerator.getAndIncrement(), CellType.SEXUAL, fullDurationMs, starvationDurationMs);
            cells.add(child);
            new Thread(child, "Cell-" + child.getId()).start();

            System.out.println("Sexual reproduction: " + parent + " + " + partner + " created " + child);
            partner.finishReproduction();
            parent.finishReproduction();
        } finally {
            reproductionLock.unlock();
        }
    }

    public int getFoodAvailable() {
        return foodResource.getAvailableFood();
    }
}
