# Stage 1 - Java Concurrency Lab

## Topic chosen: Game of Life

This project implements a Java concurrency solution inspired by the laboratory topic "Game of Life".

The application includes:

- a working concurrent simulation that models cells as threads;
- shared food resources protected by synchronization primitives;
- a faulty solution for a data race / consistency issue;
- a faulty solution for a deadlock scenario.

---

## 1. Correct solution

### Objective
The main program simulates a population of cells that:

- consume food from a shared resource;
- become hungry if they do not eat in time;
- may die if starvation lasts too long;
- reproduce after eating enough times;
- use concurrent execution through Java threads.

### Main ideas used
- Java `Thread` objects represent individual cells;
- shared food stock is protected using synchronization (`ReentrantLock`);
- atomic flags are used to coordinate state changes;
- reproduction is guarded so that cells do not create inconsistent states in parallel;
- each key event is timestamped to make the concurrency behavior easier to observe.

### Files involved
- `src/main/java/org/example/Main.java`
- `src/main/java/org/example/Simulation.java`
- `src/main/java/org/example/Cell.java`
- `src/main/java/org/example/FoodResource.java`
- `src/main/java/org/example/CellType.java`

---

## 2. Faulty solution A - Data race / consistency issue

File:
- `src/main/java/org/example/FaultyDataRaceSimulation.java`

### Problem
The program updates a shared integer (`foodBank`) from many threads without any synchronization.

This creates a classic data race:

- multiple threads read and write the same variable concurrently;
- updates are lost or inconsistent;
- the final value is unpredictable and incorrect.

### Why it is wrong
The code does:

```java
foodBank++;
foodBank--;
```

without using `synchronized`, `volatile`, `AtomicInteger`, or a lock. The result is not thread-safe.

The demo prints timestamps so the concurrent execution can be observed clearly while workers compete for the same shared resource.

### Concurrency concept demonstrated
- shared state;
- race condition;
- inconsistent final result;
- need for atomicity.

---

## 3. Faulty solution B - Deadlock

File:
- `src/main/java/org/example/FaultyDeadlockSimulation.java`

### Problem
Two threads acquire two locks in opposite orders:

- Thread 1 acquires `left`, then waits for `right`;
- Thread 2 acquires `right`, then waits for `left`.

Each thread blocks forever waiting for the other lock.

### Why it is wrong
This is a classic deadlock scenario caused by inconsistent lock ordering.

The output shows the exact moment each thread tries to acquire the locks and the point at which the deadlock is detected.

### Concurrency concept demonstrated
- deadlock;
- lock ordering problems;
- resource contention;
- need for consistent locking strategy.

---

## 4. How to run the project

From the project root:

```bash
mvn test
mvn exec:java
```

The `Main` class runs the three demonstrations:

1. working simulation;
2. faulty data race example;
3. faulty deadlock example.

