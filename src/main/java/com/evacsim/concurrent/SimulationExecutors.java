package com.evacsim.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Shared thread pools: parallel move decisions + background HTTP/DB work.
 */
public final class SimulationExecutors {

    private static final ExecutorService DECISIONS = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "evac-decide");
                t.setDaemon(true);
                return t;
            });

    private static final ExecutorService IO = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "evac-io");
        t.setDaemon(true);
        return t;
    });

    private SimulationExecutors() {
    }

    public static ExecutorService decisions() {
        return DECISIONS;
    }

    public static ExecutorService io() {
        return IO;
    }

    public static int decisionWorkers() {
        return Math.max(2, Runtime.getRuntime().availableProcessors());
    }

    public static void shutdown() {
        DECISIONS.shutdownNow();
        IO.shutdownNow();
        try {
            DECISIONS.awaitTermination(1, TimeUnit.SECONDS);
            IO.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
