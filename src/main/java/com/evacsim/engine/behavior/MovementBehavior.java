package com.evacsim.engine.behavior;

import com.evacsim.engine.MoveDecision;
import com.evacsim.model.Room;
import com.evacsim.model.Student;

import java.util.Map;

/**
 * Strategy for choosing a student's next cell each generation.
 */
public interface MovementBehavior {

    MoveDecision decide(Student self, Room room, Map<com.evacsim.model.Position, Student> occupied);
}
