package com.evacsim.engine;

import com.evacsim.model.Position;
import com.evacsim.model.Student;

/**
 * A student's requested move for the current generation.
 */
public final class MoveDecision {

    private final Student student;
    private final Position target;
    private final double score;
    private final boolean stay;

    public MoveDecision(Student student, Position target, double score, boolean stay) {
        this.student = student;
        this.target = target;
        this.score = score;
        this.stay = stay;
    }

    public static MoveDecision stay(Student student) {
        return new MoveDecision(student, student.getPosition(), 0.0, true);
    }

    public Student getStudent() {
        return student;
    }

    public Position getTarget() {
        return target;
    }

    public double getScore() {
        return score;
    }

    public boolean isStay() {
        return stay;
    }
}
