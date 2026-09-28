package com.evacsim.model;

/**
 * Immutable grid coordinate.
 */
public record Position(int row, int col) {

    public Position offset(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }

    public int manhattan(Position other) {
        return Math.abs(row - other.row) + Math.abs(col - other.col);
    }
}
