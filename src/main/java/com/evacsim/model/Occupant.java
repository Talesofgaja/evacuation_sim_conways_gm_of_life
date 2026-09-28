package com.evacsim.model;

import com.evacsim.engine.behavior.MovementBehavior;

/**
 * Abstract grid occupant. Concrete types (students, future staff) share identity and state.
 */
public abstract class Occupant {

    protected final int id;
    protected Position position;
    protected StudentState state = StudentState.NORMAL;

    protected Occupant(int id, Position position) {
        this.id = id;
        this.position = position;
    }

    public int getId() {
        return id;
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

    public boolean isPresent() {
        return state != StudentState.EVACUATED;
    }

    public boolean isStunned() {
        return state == StudentState.STUNNED;
    }

    public abstract MovementBehavior getBehavior();

    public abstract StudentType getType();
}
