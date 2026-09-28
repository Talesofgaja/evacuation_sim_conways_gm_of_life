package com.evacsim.model;

import com.evacsim.engine.behavior.AngryBehavior;
import com.evacsim.engine.behavior.MovementBehavior;
import com.evacsim.engine.behavior.ShyBehavior;

/**
 * A single evacuating student on the grid.
 */
public final class Student extends Occupant {

    private static int nextId = 1;

    private final StudentType type;
    private final MovementBehavior behavior;
    private int stunRemaining;

    public Student(StudentType type, Position position) {
        super(nextId++, position);
        this.type = type;
        this.behavior = type == StudentType.SHY ? new ShyBehavior() : new AngryBehavior();
    }

    public static void resetIdCounter() {
        nextId = 1;
    }

    @Override
    public StudentType getType() {
        return type;
    }

    @Override
    public MovementBehavior getBehavior() {
        return behavior;
    }

    public boolean isActive() {
        return state == StudentState.NORMAL;
    }

    public void stun(int generations) {
        stunRemaining = Math.max(stunRemaining, generations);
        state = StudentState.STUNNED;
    }

    public void tickStun() {
        if (state != StudentState.STUNNED) {
            return;
        }
        stunRemaining--;
        if (stunRemaining <= 0) {
            state = StudentState.NORMAL;
            stunRemaining = 0;
        }
    }

    public void evacuate() {
        state = StudentState.EVACUATED;
        stunRemaining = 0;
    }

    public int getStunRemaining() {
        return stunRemaining;
    }
}
