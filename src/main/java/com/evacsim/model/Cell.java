package com.evacsim.model;

/**
 * A single cell in the classroom grid.
 */
public final class Cell {

    private final Position position;
    private final CellType type;

    public Cell(Position position, CellType type) {
        this.position = position;
        this.type = type;
    }

    public Position getPosition() {
        return position;
    }

    public CellType getType() {
        return type;
    }

    public boolean isWalkableFloor() {
        return type == CellType.WALKABLE || type == CellType.EXIT;
    }
}
