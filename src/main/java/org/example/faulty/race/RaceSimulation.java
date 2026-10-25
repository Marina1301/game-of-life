package org.example.faulty.race;

import org.example.CellType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class RaceSimulation {
    final RaceFoodResource food;
    private final int initialFood;
    private final long fullDurationMs;
    private final long starvationDurationMs;
    private final List<RaceCell> initialCells = new ArrayList<>();


    private final List<RaceCell> cells = new ArrayList<>();
    private int aliveCount;
    private boolean running;
    private Offer waitingOffer;
    private int nextId = 1;

    private final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());
    private final Set<Integer> seenIds = ConcurrentHashMap.newKeySet();
    private final AtomicInteger auditConsumed = new AtomicInteger();
    private final AtomicInteger auditProduced = new AtomicInteger();
    private final AtomicInteger auditCreated = new AtomicInteger();
    private final AtomicInteger auditLive = new AtomicInteger();
    private final AtomicInteger listAddFailures = new AtomicInteger();
    private final AtomicInteger duplicateIds = new AtomicInteger();
    private final AtomicInteger suitorsTakenTwice = new AtomicInteger();
    private final AtomicInteger lostSuitors = new AtomicInteger();
    private final AtomicInteger sexualChildren = new AtomicInteger();
    private final AtomicInteger divisions = new AtomicInteger();


    static final class Offer {
        final RaceCell cell;
        final AtomicInteger takers = new AtomicInteger();

        Offer(RaceCell cell) {
            this.cell = cell;
        }
    }

    RaceSimulation(int cellCount, int initialFood, long fullDurationMs, long starvationDurationMs) {
        this.food = new RaceFoodResource(initialFood);
        this.initialFood = initialFood;
        this.fullDurationMs = fullDurationMs;
        this.starvationDurationMs = starvationDurationMs;
        for (int i = 0; i < cellCount; i++) {
            initialCells.add(newCell(i % 2 == 0 ? CellType.ASEXUAL : CellType.SEXUAL));
        }
    }

    void start() {
        running = true;
        for (RaceCell cell : initialCells) {
            startCell(cell);
        }

        System.out.println("[race] Simulation started with " + initialCells.size() + " cells.");
    }

    private RaceCell newCell(CellType type) {
        int id = nextId;   // R8: citire...
        Thread.yield();
        nextId = id + 1;   // ...scriere: doua fire pot primi acelasi id
        if (!seenIds.add(id)) {
            duplicateIds.incrementAndGet();
        }
        return new RaceCell(this, id, type, fullDurationMs, starvationDurationMs);
    }

    private void startCell(RaceCell cell) {
        if (!running) {
            return;
        }
        try {
            cells.add(cell); // R4: ArrayList.add concurent -> poate pierde elemente sau arunca exceptii
        } catch (RuntimeException e) {
            listAddFailures.incrementAndGet();
        }
        auditCreated.incrementAndGet();
        Thread thread = new Thread(cell, "RaceCell-" + cell.id);
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
    }

    void cellStarted() {
        auditLive.incrementAndGet();
        int current = aliveCount; // R5
        Thread.yield();
        aliveCount = current + 1;
    }

    void cellFinished() {
        auditLive.decrementAndGet();
        int current = aliveCount; // R5
        Thread.yield();
        aliveCount = current - 1;
    }

    boolean isRunning() {
        return running; // R6: citire nevolatila
    }

    void recordMeal() {
        auditConsumed.incrementAndGet();
    }

    void recordProduced(int amount) {
        auditProduced.addAndGet(amount);
    }

    boolean divide(RaceCell parent) {
        if (!running) {
            return false;
        }
        parent.alive = false;
        startCell(newCell(CellType.ASEXUAL));
        startCell(newCell(CellType.ASEXUAL));
        divisions.incrementAndGet();
        return true;
    }

    /** R7: imperechere fara nicio sincronizare. */
    boolean mate(RaceCell cell, long timeoutMs) throws InterruptedException {
        if (!running || timeoutMs <= 0) {
            return false;
        }

        Offer offer = waitingOffer;                  // check...
        if (offer != null && offer.cell != cell) {
            Thread.yield();
            waitingOffer = null;                     // ...then act: doi parteneri pot lua aceeasi oferta
            if (offer.takers.incrementAndGet() > 1) {
                suitorsTakenTwice.incrementAndGet();
            }
            offer.cell.paired = true;                // scriere simpla, citita prin polling de alt fir
            startCell(newCell(CellType.SEXUAL));
            sexualChildren.incrementAndGet();
            return true;
        }

        Offer mine = new Offer(cell);
        cell.paired = false;
        waitingOffer = mine;                         // poate suprascrie un pretendent sosit intre timp

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!cell.paired && running && System.currentTimeMillis() < deadline) {
            Thread.sleep(2);
        }
        if (cell.paired) {
            return true;
        }
        if (waitingOffer == mine) {
            waitingOffer = null;
        } else if (mine.takers.get() == 0) {
            lostSuitors.incrementAndGet();           // oferta a disparut fara sa o fi luat cineva
        }
        return false;
    }

    String status() {
        return String.format("aliveCount(racy)=%d liveThreads(audit)=%d food(racy)=%d",
                aliveCount, auditLive.get(), food.getAvailableFood());
    }

    void stop() throws InterruptedException {
        running = false; // R6: fara volatile, nimic nu garanteaza ca firele vad scrierea
        List<Thread> snapshot;
        synchronized (threads) {
            snapshot = new ArrayList<>(threads);
        }
        long deadline = System.currentTimeMillis() + 2000;
        for (Thread thread : snapshot) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining > 0) {
                thread.join(remaining);
            }
        }
    }

    void report() {
        int expectedFood = initialFood + auditProduced.get() - auditConsumed.get();
        int actualFood = food.getAvailableFood();
        int nullEntries = 0;
        for (RaceCell cell : new ArrayList<>(cells)) {
            if (cell == null) {
                nullEntries++;
            }
        }

        System.out.println();
        System.out.println("===== RACE CONDITIONS REPORT =====");
        System.out.printf("Food: expected %d (initial %d + produced %d - eaten %d), counter says %d -> %s%n",
                expectedFood, initialFood, auditProduced.get(), auditConsumed.get(), actualFood,
                verdict(expectedFood == actualFood, "lost updates (R2)"));
        System.out.printf("Lowest food value observed: %d -> %s%n", food.getLowestObserved(),
                verdict(food.getLowestObserved() >= 0, "food eaten that did not exist (R1)"));
        System.out.printf("Cells created: %d, ArrayList size: %d, null entries: %d, add exceptions: %d -> %s%n",
                auditCreated.get(), cells.size(), nullEntries, listAddFailures.get(),
                verdict(auditCreated.get() == cells.size() && nullEntries == 0 && listAddFailures.get() == 0,
                        "corrupted ArrayList (R4)"));
        System.out.printf("Population counter: %d, real live threads: %d -> %s%n",
                aliveCount, auditLive.get(), verdict(aliveCount == auditLive.get(), "non-atomic ++/-- (R5)"));
        System.out.printf("Duplicate cell ids: %d -> %s%n", duplicateIds.get(),
                verdict(duplicateIds.get() == 0, "non-atomic id generator (R8)"));
        System.out.printf("Sexual children: %d, suitors taken by 2+ partners: %d, suitors lost: %d -> %s%n",
                sexualChildren.get(), suitorsTakenTwice.get(), lostSuitors.get(),
                verdict(suitorsTakenTwice.get() == 0 && lostSuitors.get() == 0, "unsynchronized mating (R7)"));
        System.out.printf("Asexual divisions: %d%n", divisions.get());
    }

    private static String verdict(boolean ok, String bug) {
        return ok ? "OK (race did not show up this run)" : "INCONSISTENT: " + bug;
    }
}
