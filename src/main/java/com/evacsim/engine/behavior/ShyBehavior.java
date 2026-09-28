package com.evacsim.engine.behavior;

import com.evacsim.engine.MoveDecision;
import com.evacsim.model.CellType;
import com.evacsim.model.Position;
import com.evacsim.model.Room;
import com.evacsim.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Conservative mover: personal space, no bench jumping, hesitates in crowds.
 */
public final class ShyBehavior implements MovementBehavior {

    private static final double W_DIST = 3.2;
    private static final double W_CROWD = 2.4;
    private static final double W_SPACE = 1.8;
    private static final double HESITATION_DENSITY = 3;

    private static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public MoveDecision decide(Student self, Room room, Map<Position, Student> occupied) {
        Position here = self.getPosition();
        int density = room.localDensity(here, occupied, 1);
        boolean onSeat = room.getType(here) == CellType.SEAT;

        // Hesitate when crowded — but not while still trapped on a bench.
        if (!onSeat && density >= HESITATION_DENSITY
                && ThreadLocalRandom.current().nextDouble() < 0.55) {
            return MoveDecision.stay(self);
        }

        int currentDist = room.distanceToExit(here);
        List<Scored> options = new ArrayList<>();

        for (int[] d : DIRS) {
            Position next = here.offset(d[0], d[1]);
            if (!room.inBounds(next)) {
                continue;
            }
            CellType type = room.getType(next);
            if (type == CellType.WALL || type == CellType.SEAT) {
                continue; // never jump benches / walk on seats
            }
            if (occupied.containsKey(next)) {
                continue;
            }

            int nextDist = room.distanceToExit(next);
            double distGain = currentDist - nextDist;
            int nextDensity = room.localDensity(next, occupied, 1);
            int adjacentPeople = countAdjacentOccupied(next, occupied, self);

            double score = distGain * W_DIST
                    - nextDensity * W_CROWD
                    - adjacentPeople * W_SPACE
                    + ThreadLocalRandom.current().nextDouble() * 0.35;

            if (type == CellType.WALKABLE) {
                score += 0.15;
            }
            if (type == CellType.EXIT) {
                score += 4.0;
            }
            if (room.getType(here) == CellType.SEAT) {
                score += 3.0;
            }

            options.add(new Scored(next, score));
        }

        if (options.isEmpty()) {
            return MoveDecision.stay(self);
        }

        options.sort((a, b) -> Double.compare(b.score, a.score));
        Scored best = options.get(0);

        // If the best move is not an improvement and space is tight, stay put.
        if (best.score < 0.4 && density >= 2) {
            return MoveDecision.stay(self);
        }

        return new MoveDecision(self, best.pos, best.score, false);
    }

    private static int countAdjacentOccupied(Position p, Map<Position, Student> occupied, Student self) {
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

    private record Scored(Position pos, double score) {
    }
}
