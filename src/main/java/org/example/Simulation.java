package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Simulation {
    private final FoodResource foodResource;
    private final CopyOnWriteArrayList<Cell> cells = new CopyOnWriteArrayList<>();
    private final List<Thread> workerThreads = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final ReentrantLock reproductionLock = new ReentrantLock();
    private final long starvationDurationMs;

    public Simulation(int cellCount, int initialFood, long fullDurationMs, long starvationDurationMs) {
        this.foodResource = new FoodResource(initialFood);
        this.starvationDurationMs = starvationDurationMs;

        for (int i = 0; i < cellCount; i++) {
            CellType type = (i % 2 == 0) ? CellType.ASEXUAL : CellType.SEXUAL;
            Cell cell = new Cell(this, idGenerator.getAndIncrement(), type, starvationDurationMs);
            cells.add(cell);
        }
    }

    public void start() {
        if (running.getAndSet(true)) {
            return;
        }

        workerThreads.clear();
        for (Cell cell : cells) {
            startThread(cell);
        }

        System.out.println("Simulation started with " + cells.size() + " cells.");
    }

    private void startThread(Cell cell) {
        Thread thread = new Thread(cell, "Cell-" + cell.getId());
        workerThreads.add(thread);
        thread.start();
    }

    public void stop() {
        running.set(false);
        for (Thread thread : workerThreads) {
            if (thread.isAlive()) {
                try {
                    thread.join(250L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
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

        Cell child = new Cell(this, idGenerator.getAndIncrement(), parent.getType(), starvationDurationMs);
        cells.add(child);
        startThread(child);

        System.out.println("Asexual reproduction: " + parent + " created " + child);
    }

    public void reproduceSexual(Cell parent) {
        if (!parent.tryBeginReproduction()) {
            return;
        }

        reproductionLock.lock();
        Cell partner = null;
        try {
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
                return;
            }

            Cell child = new Cell(this, idGenerator.getAndIncrement(), CellType.SEXUAL, starvationDurationMs);
            cells.add(child);
            startThread(child);

            System.out.println("Sexual reproduction: " + parent + " + " + partner + " created " + child);
        } finally {
            if (partner != null) {
                partner.finishReproduction();
            }
            parent.finishReproduction();
            reproductionLock.unlock();
        }
    }

    public int getFoodAvailable() {
        return foodResource.getAvailableFood();
    }
}
