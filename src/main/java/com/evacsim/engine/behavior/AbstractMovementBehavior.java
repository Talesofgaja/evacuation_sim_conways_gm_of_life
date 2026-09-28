package com.evacsim.engine.behavior;

import com.evacsim.model.CellType;
import com.evacsim.model.Position;
import com.evacsim.model.Student;

import java.util.Map;

/**
 * Shared neighbor scoring for personality strategies.
 */
public abstract class AbstractMovementBehavior implements MovementBehavior {

    protected static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    protected boolean isImpassable(CellType type) {
        return type == CellType.WALL || type == CellType.SEAT;
    }

    protected int countAdjacentOccupied(Position p, Map<Position, Student> occupied, Student self) {
        int n = 0;
        for (int[] d : DIRS) {
            Position q = p.offset(d[0], d[1]);
            Student other = occupied.get(q);
            if (other != null && other.getId() != self.getId()) {
                n++;
            }
        }
        return n;
    }
}
