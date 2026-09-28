package com.evacsim.controller;

import com.evacsim.engine.SimulationEngine;
import com.evacsim.model.CellType;
import com.evacsim.model.Exit;
import com.evacsim.model.ExitStatus;
import com.evacsim.model.Position;
import com.evacsim.model.Room;
import com.evacsim.model.Student;
import com.evacsim.model.StudentState;
import com.evacsim.model.StudentType;
import com.evacsim.stats.SimulationStats;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * FXML controller: wires UI controls to {@link SimulationEngine} and paints the grid.
 * Contains no simulation rules.
 */
public class SimulationController {

    private static final Color COLOR_WALL = Color.web("#2a303c");
    private static final Color COLOR_FLOOR = Color.web("#1e2430");
    private static final Color COLOR_SEAT = Color.web("#3a4558");
    private static final Color COLOR_EXIT_OPEN = Color.web("#22c55e");
    private static final Color COLOR_EXIT_BUSY = Color.web("#eab308");
    private static final Color COLOR_EXIT_BLOCKED = Color.web("#ef4444");
    private static final Color COLOR_SHY = Color.web("#60a5fa");
    private static final Color COLOR_ANGRY = Color.web("#f97316");
    private static final Color COLOR_STUNNED = Color.web("#a855f7");
    private static final Color COLOR_GRID = Color.web("#151922", 0.55);

    @FXML private Canvas simulationCanvas;

    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button stepButton;
    @FXML private Button resetButton;
    @FXML private Button generateButton;

    @FXML private Slider speedSlider;
    @FXML private Slider studentCountSlider;
    @FXML private Slider shyPercentSlider;

    @FXML private Label studentCountValueLabel;
    @FXML private Label shyPercentValueLabel;
    @FXML private Label generationLabel;
    @FXML private Label remainingLabel;
    @FXML private Label evacuatedLabel;
    @FXML private Label stunnedLabel;
    @FXML private Label collisionsLabel;
    @FXML private Label exitStatusLabel;
    @FXML private Label evacTimeLabel;

    private final SimulationEngine engine = new SimulationEngine();
    private Timeline timeline;
    private boolean playing;

    @FXML
    public void initialize() {
        studentCountSlider.valueProperty().addListener((obs, o, n) ->
                studentCountValueLabel.setText("Students: " + n.intValue()));
        shyPercentSlider.valueProperty().addListener((obs, o, n) -> {
            int shy = n.intValue();
            shyPercentValueLabel.setText("Shy: " + shy + "%  ·  Angry: " + (100 - shy) + "%");
        });

        speedSlider.valueProperty().addListener((obs, o, n) -> {
            if (playing) {
                restartTimeline();
            }
        });

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        applyScenarioFromSliders();
        updateStatsLabels();
        render();
        updateButtonStates();
    }

    @FXML
    private void onPlay() {
        if (engine.isFinished()) {
            return;
        }
        playing = true;
        restartTimeline();
        updateButtonStates();
    }

    @FXML
    private void onPause() {
        playing = false;
        timeline.stop();
        updateButtonStates();
    }

    @FXML
    private void onStep() {
        if (playing) {
            onPause();
        }
        if (!engine.isFinished()) {
            engine.step();
            updateStatsLabels();
            render();
            updateButtonStates();
        }
    }

    @FXML
    private void onReset() {
        onPause();
        applyScenarioFromSliders();
        updateStatsLabels();
        render();
        updateButtonStates();
    }

    @FXML
    private void onGenerateScenario() {
        onPause();
        engine.randomizeScenario();
        studentCountSlider.setValue(engine.getStats().getInitialCount());
        int shy = 0;
        for (Student s : engine.getStudents()) {
            if (s.getType() == StudentType.SHY) {
                shy++;
            }
        }
        int total = engine.getStats().getInitialCount();
        shyPercentSlider.setValue(total == 0 ? 50 : (100.0 * shy / total));
        updateStatsLabels();
        render();
        updateButtonStates();
    }

    private void applyScenarioFromSliders() {
        int count = (int) Math.round(studentCountSlider.getValue());
        double shyRatio = shyPercentSlider.getValue() / 100.0;
        engine.reset(count, shyRatio);
    }

    private void restartTimeline() {
        timeline.stop();
        double gensPerSec = Math.max(1.0, speedSlider.getValue());
        Duration frame = Duration.millis(1000.0 / gensPerSec);
        timeline.getKeyFrames().setAll(new KeyFrame(frame, e -> {
            engine.step();
            updateStatsLabels();
            render();
            if (engine.isFinished()) {
                onPause();
            }
        }));
        timeline.play();
    }

    private void updateButtonStates() {
        boolean done = engine.isFinished();
        playButton.setDisable(playing || done);
        pauseButton.setDisable(!playing);
        stepButton.setDisable(playing || done);
    }

    private void updateStatsLabels() {
        SimulationStats s = engine.getStats();
        generationLabel.setText("Generation: " + s.getGeneration());
        remainingLabel.setText("Students Remaining: " + s.getRemaining());
        evacuatedLabel.setText("Evacuated: " + s.getEvacuated() + " / " + s.getInitialCount());
        stunnedLabel.setText("Stunned: " + s.getStunned());
        collisionsLabel.setText("Collisions: " + s.getCollisions());
        exitStatusLabel.setText("Exit Status: " + s.getExitStatusSummary());
        if (s.getEvacuationTime() >= 0) {
            evacTimeLabel.setText("Evacuation Time: " + s.getEvacuationTime() + " gens");
        } else {
            evacTimeLabel.setText("Evacuation Time: —");
        }
    }

    private void render() {
        GraphicsContext gc = simulationCanvas.getGraphicsContext2D();
        double width = simulationCanvas.getWidth();
        double height = simulationCanvas.getHeight();
        gc.setFill(Color.web("#12151c"));
        gc.fillRect(0, 0, width, height);

        Room room = engine.getRoom();
        double cellW = width / Room.COLS;
        double cellH = height / Room.ROWS;
        double cell = Math.min(cellW, cellH);
        double offsetX = (width - cell * Room.COLS) / 2.0;
        double offsetY = (height - cell * Room.ROWS) / 2.0;

        // Floor / walls / seats
        for (int r = 0; r < Room.ROWS; r++) {
            for (int c = 0; c < Room.COLS; c++) {
                Position p = new Position(r, c);
                CellType type = room.getType(p);
                Color fill = switch (type) {
                    case WALL -> COLOR_WALL;
                    case SEAT -> COLOR_SEAT;
                    case EXIT -> COLOR_EXIT_OPEN;
                    case WALKABLE -> COLOR_FLOOR;
                };
                gc.setFill(fill);
                gc.fillRect(offsetX + c * cell, offsetY + r * cell, cell, cell);
            }
        }

        // Exit status colors override EXIT cells.
        for (Exit exit : room.getExits()) {
            Color color = switch (exit.getStatus()) {
                case OPEN -> COLOR_EXIT_OPEN;
                case CONGESTED -> COLOR_EXIT_BUSY;
                case BLOCKED -> COLOR_EXIT_BLOCKED;
            };
            gc.setFill(color);
            for (Position p : exit.getCells()) {
                gc.fillRect(offsetX + p.col() * cell, offsetY + p.row() * cell, cell, cell);
            }
        }

        // Subtle grid lines
        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(0.6);
        for (int r = 0; r <= Room.ROWS; r++) {
            double y = offsetY + r * cell;
            gc.strokeLine(offsetX, y, offsetX + Room.COLS * cell, y);
        }
        for (int c = 0; c <= Room.COLS; c++) {
            double x = offsetX + c * cell;
            gc.strokeLine(x, offsetY, x, offsetY + Room.ROWS * cell);
        }

        // Students
        double pad = cell * 0.14;
        for (Student student : engine.getStudents()) {
            if (!student.isPresent()) {
                continue;
            }
            Position p = student.getPosition();
            Color color;
            if (student.getState() == StudentState.STUNNED) {
                color = COLOR_STUNNED;
            } else if (student.getType() == StudentType.SHY) {
                color = COLOR_SHY;
            } else {
                color = COLOR_ANGRY;
            }
            gc.setFill(color);
            gc.fillRoundRect(
                    offsetX + p.col() * cell + pad,
                    offsetY + p.row() * cell + pad,
                    cell - 2 * pad,
                    cell - 2 * pad,
                    3, 3);
        }
    }
}
