package com.evacsim.stats;

/**
 * Live counters for the current simulation run.
 */
public final class SimulationStats {

    private int generation;
    private int initialCount;
    private int remaining;
    private int evacuated;
    private int stunned;
    private int collisions;
    private int evacuationTime = -1;
    private String exitStatusSummary = "—";

    public void reset(int initialCount) {
        this.initialCount = initialCount;
        this.generation = 0;
        this.remaining = initialCount;
        this.evacuated = 0;
        this.stunned = 0;
        this.collisions = 0;
        this.evacuationTime = -1;
        this.exitStatusSummary = "OPEN";
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public void setEvacuated(int evacuated) {
        this.evacuated = evacuated;
    }

    public void setStunned(int stunned) {
        this.stunned = stunned;
    }

    public void addCollisions(int n) {
        this.collisions += n;
    }

    public void setEvacuationTime(int evacuationTime) {
        this.evacuationTime = evacuationTime;
    }

    public void setExitStatusSummary(String exitStatusSummary) {
        this.exitStatusSummary = exitStatusSummary;
    }

    public int getGeneration() {
        return generation;
    }

    public int getInitialCount() {
        return initialCount;
    }

    public int getRemaining() {
        return remaining;
    }

    public int getEvacuated() {
        return evacuated;
    }

    public int getStunned() {
        return stunned;
    }

    public int getCollisions() {
        return collisions;
    }

    public int getEvacuationTime() {
        return evacuationTime;
    }

    public String getExitStatusSummary() {
        return exitStatusSummary;
    }

    public boolean isComplete() {
        return remaining == 0 && initialCount > 0;
    }
}
