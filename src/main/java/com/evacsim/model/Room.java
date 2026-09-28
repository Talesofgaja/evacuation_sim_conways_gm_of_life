package com.evacsim.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Classroom grid: walls, aisles, bench/seat columns (~120 seats), and exits.
 */
public final class Room {

    public static final int ROWS = 24;
    public static final int COLS = 20;

    private final Cell[][] cells;
    private final List<Position> seats;
    private final List<Exit> exits;
    private final int[][] distanceField;

    public Room() {
        this.cells = new Cell[ROWS][COLS];
        this.seats = new ArrayList<>();
        this.exits = new ArrayList<>();
        buildLayout();
        this.distanceField = computeDistanceField();
    }

    private void buildLayout() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                CellType type = CellType.WALKABLE;
                if (r == 0 || c == 0 || c == COLS - 1 || r == ROWS - 1) {
                    type = CellType.WALL;
                }
                cells[r][c] = new Cell(new Position(r, c), type);
            }
        }

        // Four bench columns (2 seats wide) → 16 seat-rows × 8 = 128 seats.
        int[][] benchCols = {{3, 4}, {7, 8}, {11, 12}, {15, 16}};
        int seatStart = 2;
        int seatEnd = 17;
        for (int r = seatStart; r <= seatEnd; r++) {
            for (int[] pair : benchCols) {
                for (int c : pair) {
                    Position p = new Position(r, c);
                    cells[r][c] = new Cell(p, CellType.SEAT);
                    seats.add(p);
                }
            }
        }

        addExit(0, List.of(new Position(ROWS - 1, 5), new Position(ROWS - 1, 6)));
        addExit(1, List.of(new Position(ROWS - 1, 13), new Position(ROWS - 1, 14)));
        addExit(2, List.of(new Position(10, 0), new Position(11, 0)));
    }

    private void addExit(int id, List<Position> exitCells) {
        for (Position p : exitCells) {
            cells[p.row()][p.col()] = new Cell(p, CellType.EXIT);
        }
        List<Position> approach = Exit.buildApproach(exitCells, ROWS, COLS);
        exits.add(new Exit(id, exitCells, approach));
    }

    /**
     * Multi-source BFS distance to the nearest currently passable exit cell.
     */
    public int[][] computeDistanceField() {
        int[][] dist = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                dist[r][c] = Integer.MAX_VALUE / 4;
            }
        }

        ArrayDeque<Position> queue = new ArrayDeque<>();
        for (Exit exit : exits) {
            if (!exit.isPassable()) {
                continue;
            }
            for (Position p : exit.getCells()) {
                dist[p.row()][p.col()] = 0;
                queue.offer(p);
            }
        }

        if (queue.isEmpty()) {
            for (Exit exit : exits) {
                for (Position p : exit.getCells()) {
                    dist[p.row()][p.col()] = 0;
                    queue.offer(p);
                }
            }
        }

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            Position cur = queue.poll();
            int base = dist[cur.row()][cur.col()];
            for (int[] d : dirs) {
                int nr = cur.row() + d[0];
                int nc = cur.col() + d[1];
                if (!inBounds(nr, nc)) {
                    continue;
                }
                CellType type = cells[nr][nc].getType();
                // Seats/benches are obstacles for path distance so aisle routes win.
                // Agents on seats get a derived distance after the flood fill.
                if (type == CellType.WALL || type == CellType.SEAT) {
                    continue;
                }
                int nd = base + 1;
                if (nd < dist[nr][nc]) {
                    dist[nr][nc] = nd;
                    queue.offer(new Position(nr, nc));
                }
            }
        }

        // Derive seat distances from neighboring aisle/exit cells.
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (cells[r][c].getType() != CellType.SEAT) {
                    continue;
                }
                int best = Integer.MAX_VALUE / 4;
                for (int[] d : dirs) {
                    int nr = r + d[0];
                    int nc = c + d[1];
                    if (inBounds(nr, nc)) {
                        best = Math.min(best, dist[nr][nc] + 1);
                    }
                }
                dist[r][c] = best;
            }
        }
        return dist;
    }

    public void refreshDistanceField() {
        int[][] fresh = computeDistanceField();
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(fresh[r], 0, distanceField[r], 0, COLS);
        }
    }

    public int distanceToExit(Position p) {
        return distanceField[p.row()][p.col()];
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public boolean inBounds(Position p) {
        return inBounds(p.row(), p.col());
    }

    public Cell getCell(Position p) {
        return cells[p.row()][p.col()];
    }

    public CellType getType(Position p) {
        return cells[p.row()][p.col()].getType();
    }

    public List<Position> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public List<Exit> getExits() {
        return Collections.unmodifiableList(exits);
    }

    public Optional<Exit> exitAt(Position p) {
        for (Exit exit : exits) {
            if (exit.getCells().contains(p)) {
                return Optional.of(exit);
            }
        }
        return Optional.empty();
    }

    public boolean isExitCell(Position p) {
        return getType(p) == CellType.EXIT;
    }

    public Map<Position, Student> occupancyMap(List<Student> students) {
        Map<Position, Student> map = new HashMap<>();
        for (Student s : students) {
            if (s.isPresent()) {
                map.put(s.getPosition(), s);
            }
        }
        return map;
    }

    public int localDensity(Position center, Map<Position, Student> occupied, int radius) {
        int count = 0;
        for (int r = center.row() - radius; r <= center.row() + radius; r++) {
            for (int c = center.col() - radius; c <= center.col() + radius; c++) {
                if (!inBounds(r, c)) {
                    continue;
                }
                Position p = new Position(r, c);
                if (!p.equals(center) && occupied.containsKey(p)) {
                    count++;
                }
            }
        }
        return count;
    }
}
