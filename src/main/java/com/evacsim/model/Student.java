package com.evacsim.model;

import com.evacsim.engine.behavior.AngryBehavior;
import com.evacsim.engine.behavior.MovementBehavior;
import com.evacsim.engine.behavior.ShyBehavior;

/**
 * A single evacuating student on the grid.
 */
public final class Student {

    private static int nextId = 1;

    private final int id;
    private final StudentType type;
    private final MovementBehavior behavior;
    private Position position;
    private StudentState state = StudentState.NORMAL;
    private int stunRemaining;

    public Student(StudentType type, Position position) {
        this.id = nextId++;
        this.type = type;
        this.position = position;
        this.behavior = type == StudentType.SHY ? new ShyBehavior() : new AngryBehavior();
    }

    public static void resetIdCounter() {
        nextId = 1;
    }

    public int getId() {
        return id;
    }

    public StudentType getType() {
        return type;
    }

    public MovementBehavior getBehavior() {
        return behavior;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public StudentState getState() {
        return state;
    }

    public boolean isActive() {
        return state == StudentState.NORMAL;
    }

    public boolean isPresent() {
        return state != StudentState.EVACUATED;
    }

    public boolean isStunned() {
        return state == StudentState.STUNNED;
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
