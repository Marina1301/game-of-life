package org.example.faulty.deadlock;

import org.example.CellType;
import org.example.FoodResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

final class DeadlockSimulation {
    private static final long HOLD_MS = 20;    
    private static final long PARTNER_POLL_MS = 5;

    private final long fullDurationMs;
    private final long starvationDurationMs;
    private final FoodResource food;

    private final Map<DeadlockCell, Thread> activeCells = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<Integer, DeadlockCell> seekers = new ConcurrentSkipListMap<>();

    private final List<DeadlockCell> initialCells = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private final LongAdder meals = new LongAdder();
    private final LongAdder divisions = new LongAdder();
    private final LongAdder mateAttempts = new LongAdder();
    private final LongAdder sexualChildren = new LongAdder();
    private int threadsStuckAfterStop;

    DeadlockSimulation(int cellCount, int initialFood,
                       long fullDurationMs, long starvationDurationMs) {
        this.fullDurationMs = fullDurationMs;
        this.starvationDurationMs = starvationDurationMs;
        this.food = new FoodResource(initialFood);
        for (int i = 0; i < cellCount; i++) {
            CellType type = (i % 2 == 0) ? CellType.ASEXUAL : CellType.SEXUAL;
            initialCells.add(newCell(type));
        }
    }

    private DeadlockCell newCell(CellType type) {
        return new DeadlockCell(this, idGenerator.getAndIncrement(), type, fullDurationMs, starvationDurationMs);
    }

    void start() {
        if (running.getAndSet(true)) {
            return;
        }
        for (DeadlockCell cell : initialCells) {
            startCell(cell);
        }
        System.out.println("[deadlock] Simulation started with " + initialCells.size() + " cells.");
    }

    private void startCell(DeadlockCell cell) {
        if (!running.get()) {
            return;
        }
        Thread thread = new Thread(cell, "DeadlockCell-" + cell.getId());
        thread.setDaemon(true);
        activeCells.put(cell, thread);
        thread.start();
        if (!running.get()) {
            thread.interrupt();
        }
    }

    void onCellFinished(DeadlockCell cell) {
        activeCells.remove(cell);
    }

    boolean isRunning() {
        return running.get();
    }

    boolean tryConsumeFood(long timeoutMs) throws InterruptedException {
        return food.tryConsume(timeoutMs);
    }

    void addFood(int amount) {
        food.addFood(amount);
    }

    void recordMeal() {
        meals.increment();
    }

    boolean divide(DeadlockCell parent) {
        if (!running.get() || !parent.deactivate()) {
            return false;
        }
        startCell(newCell(CellType.ASEXUAL));
        startCell(newCell(CellType.ASEXUAL));
        divisions.increment();
        return true;
    }

    boolean mate(DeadlockCell cell, long timeoutMs) throws InterruptedException {
        if (!running.get() || timeoutMs <= 0) {
            return false;
        }
        long deadline = System.currentTimeMillis() + timeoutMs;
        seekers.put(cell.getId(), cell);
        try {
            DeadlockCell partner = awaitPartner(cell, deadline);
            if (partner == null) {
                return false;
            }
            mateAttempts.increment();
            synchronized (cell) {
                Thread.sleep(HOLD_MS);
                synchronized (partner) {
                    if (cell.getId() < partner.getId()) {
                        startCell(newCell(CellType.SEXUAL));
                        sexualChildren.increment();
                    }
                    return true;
                }
            }
        } finally {
            seekers.remove(cell.getId());
        }
    }

    private DeadlockCell awaitPartner(DeadlockCell cell, long deadline) throws InterruptedException {
        while (running.get() && System.currentTimeMillis() < deadline) {
            for (DeadlockCell candidate : seekers.values()) {
                if (candidate != cell) {
                    return candidate;
                }
            }
            Thread.sleep(PARTNER_POLL_MS);
        }
        return null;
    }

    void stop() {
        running.set(false);
        List<Thread> threads = new ArrayList<>(activeCells.values());
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            try {
                thread.join(300L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        int stuck = 0;
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                stuck++;
            }
        }
        threadsStuckAfterStop = stuck;
    }

    String status() {
        return String.format("alive=%d meals=%d mateAttempts=%d sexualChildren=%d food=%d",
                activeCells.size(), meals.sum(), mateAttempts.sum(), sexualChildren.sum(), food.getAvailableFood());
    }

    void report() {
        System.out.println();
        System.out.println("===== DEADLOCK REPORT =====");
        System.out.printf("Meals: %d, asexual divisions: %d%n", meals.sum(), divisions.sum());
        System.out.printf("Sexual mating attempts: %d, sexual children: %d%n",
                mateAttempts.sum(), sexualChildren.sum());
        System.out.printf("Threads still alive after stop(): %d (BLOCKED on monitors, interrupt() cannot free them)%n",
                threadsStuckAfterStop);
    }
}