package com.evacsim.engine;

import com.evacsim.model.Exit;
import com.evacsim.model.ExitStatus;
import com.evacsim.model.Position;
import com.evacsim.model.Room;
import com.evacsim.model.Student;
import com.evacsim.model.StudentType;
import com.evacsim.stats.SimulationStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Discrete-generation evacuation engine (Game-of-Life style step function).
 * Owns the room and students; contains no JavaFX code.
 */
public final class SimulationEngine {

    private static final int STUN_DURATION = 3;
    private static final int EXIT_BLOCK_ON_COLLISION = 4;

    private Room room;
    private final List<Student> students = new ArrayList<>();
    private final SimulationStats stats = new SimulationStats();
    private boolean finished;

    public SimulationEngine() {
        reset(70, 0.70);
    }

    public Room getRoom() {
        return room;
    }

    public List<Student> getStudents() {
        return Collections.unmodifiableList(students);
    }

    public SimulationStats getStats() {
        return stats;
    }

    public boolean isFinished() {
        return finished;
    }

    public void reset(int studentCount, double shyRatio) {
        Student.resetIdCounter();
        room = new Room();
        students.clear();
        finished = false;

        int count = Math.max(1, Math.min(studentCount, room.getSeats().size()));
        List<Position> seats = new ArrayList<>(room.getSeats());
        Collections.shuffle(seats, ThreadLocalRandom.current());

        int shyCount = (int) Math.round(count * shyRatio);
        for (int i = 0; i < count; i++) {
            StudentType type = i < shyCount ? StudentType.SHY : StudentType.ANGRY;
            students.add(new Student(type, seats.get(i)));
        }

        stats.reset(count);
        refreshStats();
    }

    public void randomizeScenario() {
        int count = 60 + ThreadLocalRandom.current().nextInt(21);
        double shy = ThreadLocalRandom.current().nextDouble();
        reset(count, shy);
    }

    /**
     * Advance exactly one generation.
     */
    public void step() {
        if (finished) {
            return;
        }

        // Recover from prior-generation stuns before deciding moves.
        for (Student s : students) {
            if (s.isPresent() && s.isStunned()) {
                s.tickStun();
            }
        }

        room.refreshDistanceField();
        Map<Position, Student> occupied = room.occupancyMap(students);

        List<MoveDecision> decisions = new ArrayList<>();
        for (Student s : students) {
            if (!s.isPresent()) {
                continue;
            }
            if (s.isStunned()) {
                decisions.add(MoveDecision.stay(s));
            } else {
                decisions.add(s.getBehavior().decide(s, room, occupied));
            }
        }

        Map<Position, List<MoveDecision>> byTarget = new HashMap<>();
        for (MoveDecision d : decisions) {
            if (d.isStay()) {
                continue;
            }
            byTarget.computeIfAbsent(d.getTarget(), k -> new ArrayList<>()).add(d);
        }

        Set<Student> movers = new HashSet<>();
        // Cells that will be vacated by approved movers this generation.
        Set<Position> vacating = new HashSet<>();
        // Cells claimed as destinations this generation.
        Set<Position> destinations = new HashSet<>();

        Map<Integer, Position> intendedTarget = new HashMap<>();
        for (MoveDecision d : decisions) {
            if (!d.isStay()) {
                intendedTarget.put(d.getStudent().getId(), d.getTarget());
            }
        }

        int collisionsThisStep = 0;

        // Process targets sorted for determinism.
        List<Position> targets = new ArrayList<>(byTarget.keySet());
        targets.sort((a, b) -> {
            int cmp = Integer.compare(a.row(), b.row());
            return cmp != 0 ? cmp : Integer.compare(a.col(), b.col());
        });

        for (Position target : targets) {
            List<MoveDecision> contenders = byTarget.get(target);
            Student occupant = occupied.get(target);
            boolean free = occupant == null
                    || intendedTarget.containsKey(occupant.getId());

            if (!free || destinations.contains(target)) {
                continue;
            }

            if (contenders.size() == 1) {
                approve(contenders.get(0), movers, vacating, destinations);
                continue;
            }

            List<MoveDecision> angry = contenders.stream()
                    .filter(d -> d.getStudent().getType() == StudentType.ANGRY)
                    .collect(Collectors.toList());
            List<MoveDecision> shy = contenders.stream()
                    .filter(d -> d.getStudent().getType() == StudentType.SHY)
                    .collect(Collectors.toList());

            if (angry.size() >= 2) {
                collisionsThisStep++;
                // Soften pure deadlock: one angry may shove through; the rest stun.
                angry.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
                MoveDecision shover = angry.get(0);
                for (int i = 1; i < angry.size(); i++) {
                    angry.get(i).getStudent().stun(STUN_DURATION);
                }
                // Winner also risks a short stun if the fight was fierce.
                if (angry.size() >= 3 || ThreadLocalRandom.current().nextDouble() < 0.35) {
                    shover.getStudent().stun(STUN_DURATION);
                    maybeBlockNearbyExit(target);
                } else {
                    approve(shover, movers, vacating, destinations);
                    maybeBlockNearbyExit(target);
                }
            } else if (angry.size() == 1) {
                approve(angry.get(0), movers, vacating, destinations);
            } else {
                shy.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
                MoveDecision best = shy.get(0);
                boolean clearLead = shy.size() == 1
                        || best.getScore() > shy.get(1).getScore() + 0.75;
                if (clearLead) {
                    approve(best, movers, vacating, destinations);
                }
            }
        }

        // Evacuate students who reached a passable exit.
        for (Student s : students) {
            if (!s.isPresent() || s.isStunned()) {
                continue;
            }
            Position p = s.getPosition();
            if (room.isExitCell(p)) {
                room.exitAt(p).ifPresent(exit -> {
                    if (exit.isPassable()) {
                        s.evacuate();
                    }
                });
            }
        }

        updateExits();

        stats.setGeneration(stats.getGeneration() + 1);
        stats.addCollisions(collisionsThisStep);
        refreshStats();

        if (stats.getRemaining() == 0) {
            finished = true;
            if (stats.getEvacuationTime() < 0) {
                stats.setEvacuationTime(stats.getGeneration());
            }
        }
    }

    private void approve(MoveDecision decision, Set<Student> movers,
                         Set<Position> vacating, Set<Position> destinations) {
        Student s = decision.getStudent();
        if (movers.contains(s) || s.isStunned()) {
            return;
        }
        Position from = s.getPosition();
        Position to = decision.getTarget();
        if (destinations.contains(to)) {
            return;
        }
        vacating.add(from);
        destinations.add(to);
        s.setPosition(to);
        movers.add(s);
    }

    private void maybeBlockNearbyExit(Position collisionCell) {
        for (Exit exit : room.getExits()) {
            if (exit.getCells().contains(collisionCell)
                    || exit.getApproachZone().contains(collisionCell)) {
                exit.setBlocked(EXIT_BLOCK_ON_COLLISION);
            }
        }
    }

    private void updateExits() {
        Map<Position, Student> occupied = room.occupancyMap(students);
        StringBuilder summary = new StringBuilder();
        for (Exit exit : room.getExits()) {
            int inApproach = 0;
            int stunned = 0;
            for (Position p : exit.getApproachZone()) {
                Student s = occupied.get(p);
                if (s != null) {
                    inApproach++;
                    if (s.isStunned()) {
                        stunned++;
                    }
                }
            }
            for (Position p : exit.getCells()) {
                Student s = occupied.get(p);
                if (s != null && s.isStunned()) {
                    stunned++;
                    exit.setBlocked(EXIT_BLOCK_ON_COLLISION);
                }
            }
            exit.tickAndRecompute(inApproach, stunned);
            if (summary.length() > 0) {
                summary.append(" · ");
            }
            summary.append("E").append(exit.getId() + 1)
                    .append(":").append(shortStatus(exit.getStatus()));
        }
        stats.setExitStatusSummary(summary.toString());
    }

    private static String shortStatus(ExitStatus status) {
        return switch (status) {
            case OPEN -> "OPEN";
            case CONGESTED -> "BUSY";
            case BLOCKED -> "BLOCKED";
        };
    }

    private void refreshStats() {
        int remaining = 0;
        int evacuated = 0;
        int stunned = 0;
        for (Student s : students) {
            switch (s.getState()) {
                case NORMAL -> remaining++;
                case STUNNED -> {
                    remaining++;
                    stunned++;
                }
                case EVACUATED -> evacuated++;
            }
        }
        stats.setRemaining(remaining);
        stats.setEvacuated(evacuated);
        stats.setStunned(stunned);
    }
}
