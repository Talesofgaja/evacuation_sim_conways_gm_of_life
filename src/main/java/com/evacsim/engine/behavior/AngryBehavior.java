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
 * Aggressive mover: lower crowd aversion, competes for cells, can jump benches.
 */
public final class AngryBehavior extends AbstractMovementBehavior {

    private static final double W_DIST = 4.0;
    private static final double W_CROWD = 0.7;
    private static final double JUMP_BONUS = 1.6;

    @Override
    public MoveDecision decide(Student self, Room room, Map<Position, Student> occupied) {
        Position here = self.getPosition();
        int currentDist = room.distanceToExit(here);
        List<Scored> options = new ArrayList<>();

        for (int[] d : DIRS) {
            Position next = here.offset(d[0], d[1]);
            scoreCandidate(self, room, occupied, here, currentDist, next, false, options);
        }

        for (int[] d : DIRS) {
            Position over = here.offset(d[0], d[1]);
            Position landing = here.offset(d[0] * 2, d[1] * 2);
            if (!room.inBounds(over) || !room.inBounds(landing)) {
                continue;
            }
            if (room.getType(over) != CellType.SEAT) {
                continue;
            }
            CellType landType = room.getType(landing);
            if (isImpassable(landType)) {
                continue;
            }
            scoreCandidate(self, room, occupied, here, currentDist, landing, true, options);
        }

        if (options.isEmpty()) {
            return MoveDecision.stay(self);
        }

        options.sort((a, b) -> Double.compare(b.score, a.score));
        Scored best = options.get(0);
        if (best.score < -1.0) {
            return MoveDecision.stay(self);
        }
        return new MoveDecision(self, best.pos, best.score, false);
    }

    private void scoreCandidate(Student self, Room room, Map<Position, Student> occupied,
                                Position here, int currentDist, Position next,
                                boolean jump, List<Scored> options) {
        if (!room.inBounds(next)) {
            return;
        }
        CellType type = room.getType(next);
        if (isImpassable(type)) {
            return;
        }
        if (occupied.containsKey(next) && !next.equals(here)) {
            return;
        }

        int nextDist = room.distanceToExit(next);
        double distGain = currentDist - nextDist;
        int nextDensity = room.localDensity(next, occupied, 1);

        double score = distGain * W_DIST
                - nextDensity * W_CROWD
                + ThreadLocalRandom.current().nextDouble() * 0.8;
        if (jump) {
            score += JUMP_BONUS;
        }
        if (type == CellType.EXIT) {
            score += 5.0;
        }
        if (room.getType(here) == CellType.SEAT && !jump) {
            score += 2.0;
        }

        options.add(new Scored(next, score));
    }

    private record Scored(Position pos, double score) {
    }
}
