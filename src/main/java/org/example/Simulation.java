package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public final class Simulation {
    private final FoodResource foodResource;
    private final Map<Cell, Thread> activeCells = new ConcurrentHashMap<>();
    private final ReentrantLock matingLock = new ReentrantLock();
    private final Condition mated = matingLock.newCondition();
    private Cell waitingSuitor;

    private final List<Cell> initialCells = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private final long fullDurationMs;       // T_full
    private final long starvationDurationMs; // T_starve

    public Simulation(int cellCount, int initialFood, long fullDurationMs, long starvationDurationMs) {
        this.foodResource = new FoodResource(initialFood);
        this.fullDurationMs = fullDurationMs;
        this.starvationDurationMs = starvationDurationMs;

        for (int i = 0; i < cellCount; i++) {
            CellType type = (i % 2 == 0) ? CellType.ASEXUAL : CellType.SEXUAL;
            initialCells.add(newCell(type));
        }
    }

    private Cell newCell(CellType type) {
        return new Cell(this, idGenerator.getAndIncrement(), type, fullDurationMs, starvationDurationMs);
    }

    public void start() {
        if (running.getAndSet(true)) {
            return;
        }
        for (Cell cell : initialCells) {
            startCell(cell);
        }
        initialCells.clear();
        System.out.println("Simulation started with " + activeCells.size() + " cells.");
    }

    private void startCell(Cell cell) {
        if (!running.get()) {
            return;
        }
        Thread thread = new Thread(cell, "Cell-" + cell.getId());
        activeCells.put(cell, thread);
        thread.start();

        if (!running.get()) {
            thread.interrupt();
        }
    }

    void onCellFinished(Cell cell) {
        activeCells.remove(cell);
    }

    public void stop() {
        running.set(false);
        List<Thread> threads = new ArrayList<>(activeCells.values());
        for (Thread thread : threads) {
            thread.interrupt(); 
        }
        for (Thread thread : threads) {
            try {
                thread.join(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean tryConsumeFood(long timeoutMs) throws InterruptedException {
        return foodResource.tryConsume(timeoutMs);
    }

    public void addFood(int amount) {
        foodResource.addFood(amount);
    }


    public boolean divide(Cell parent) {
        if (!running.get() || !parent.deactivate()) {
            return false;
        }
        Cell child1 = newCell(parent.getType());
        Cell child2 = newCell(parent.getType());
        startCell(child1);
        startCell(child2);
        System.out.println("Asexual division: " + parent + " -> " + child1 + " + " + child2);
        return true;
    }

    public boolean mate(Cell cell, long timeoutMs) throws InterruptedException {
        if (!running.get() || timeoutMs <= 0) {
            return false;
        }

        Cell partner;
        matingLock.lock();
        try {
            cell.resetPaired();

            if (waitingSuitor == null) {
                waitingSuitor = cell;
                try {
                    long remaining = timeoutMs * 1_000_000L;
                    while (!cell.isPaired() && remaining > 0 && running.get()) {
                        remaining = mated.awaitNanos(remaining);
                    }
                    return cell.isPaired();
                } finally {
                    if (waitingSuitor == cell) {
                        waitingSuitor = null;
                    }
                }
            }

            partner = waitingSuitor;
            waitingSuitor = null;
            partner.markPaired();
            mated.signal();
        } finally {
            matingLock.unlock();
        }

        Cell child = newCell(CellType.SEXUAL);
        startCell(child);
        if (running.get()) {
            System.out.println("Sexual reproduction: " + partner + " + " + cell + " -> " + child);
        }
        return true;
    }

    public int getFoodAvailable() {
        return foodResource.getAvailableFood();
    }

    public int getAliveCellCount() {
        return activeCells.size();
    }
}
