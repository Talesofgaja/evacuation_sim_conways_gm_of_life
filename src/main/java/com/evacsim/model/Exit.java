package com.evacsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An exit doorway and the cells that form its approach zone.
 */
public final class Exit {

    private final int id;
    private final List<Position> cells;
    private final List<Position> approachZone;
    private ExitStatus status = ExitStatus.OPEN;
    private int blockRemaining;

    public Exit(int id, List<Position> cells, List<Position> approachZone) {
        this.id = id;
        this.cells = List.copyOf(cells);
        this.approachZone = List.copyOf(approachZone);
    }

    public int getId() {
        return id;
    }

    public List<Position> getCells() {
        return cells;
    }

    public List<Position> getApproachZone() {
        return approachZone;
    }

    public ExitStatus getStatus() {
        return status;
    }

    public boolean isPassable() {
        return status != ExitStatus.BLOCKED;
    }

    public void setBlocked(int generations) {
        blockRemaining = Math.max(blockRemaining, generations);
        status = ExitStatus.BLOCKED;
    }

    public void tickAndRecompute(int studentsInApproach, int stunnedInApproach) {
        if (blockRemaining > 0) {
            blockRemaining--;
            status = ExitStatus.BLOCKED;
            return;
        }

        if (stunnedInApproach > 0 || studentsInApproach >= 5) {
            status = ExitStatus.BLOCKED;
        } else if (studentsInApproach >= 3) {
            status = ExitStatus.CONGESTED;
        } else {
            status = ExitStatus.OPEN;
        }
    }

    public Position nearestCell(Position from) {
        Position best = cells.get(0);
        int bestDist = from.manhattan(best);
        for (Position cell : cells) {
            int d = from.manhattan(cell);
            if (d < bestDist) {
                best = cell;
                bestDist = d;
            }
        }
        return best;
    }

    public static List<Position> buildApproach(List<Position> exitCells, int rows, int cols) {
        List<Position> zone = new ArrayList<>();
        for (Position exit : exitCells) {
            for (int dr = -2; dr <= 2; dr++) {
                for (int dc = -2; dc <= 2; dc++) {
                    int r = exit.row() + dr;
                    int c = exit.col() + dc;
                    if (r >= 0 && r < rows && c >= 0 && c < cols) {
                        Position p = new Position(r, c);
                        if (!exitCells.contains(p) && !zone.contains(p)) {
                            zone.add(p);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableList(zone);
    }
}
