package com.evacsim.controller;

import com.evacsim.concurrent.SimulationExecutors;
import com.evacsim.db.Operator;
import com.evacsim.db.RunDao;
import com.evacsim.db.SavedScenario;
import com.evacsim.db.ScenarioDao;
import com.evacsim.db.SimulationRun;
import com.evacsim.engine.SimulationEngine;
import com.evacsim.model.CellType;
import com.evacsim.model.Exit;
import com.evacsim.model.HazardConditions;
import com.evacsim.model.Position;
import com.evacsim.model.Room;
import com.evacsim.model.Student;
import com.evacsim.model.StudentState;
import com.evacsim.model.StudentType;
import com.evacsim.net.WeatherService;
import com.evacsim.stats.SimulationStats;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;
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

    @FXML private StackPane canvasHolder;
    @FXML private Canvas simulationCanvas;
    @FXML private SplitPane splitPane;

    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button stepButton;
    @FXML private Button resetButton;
    @FXML private Button generateButton;
    @FXML private Button fetchWeatherButton;

    @FXML private Slider speedSlider;
    @FXML private Slider studentCountSlider;
    @FXML private Slider shyPercentSlider;

    @FXML private Label titleLabel;
    @FXML private Label studentCountValueLabel;
    @FXML private Label shyPercentValueLabel;
    @FXML private Label generationLabel;
    @FXML private Label remainingLabel;
    @FXML private Label evacuatedLabel;
    @FXML private Label stunnedLabel;
    @FXML private Label collisionsLabel;
    @FXML private Label exitStatusLabel;
    @FXML private Label evacTimeLabel;
    @FXML private Label hazardStatusLabel;
    @FXML private Label operatorLabel;
    @FXML private Label poolLabel;
    @FXML private Label statusLabel;
    @FXML private Label weatherLabel;
    @FXML private Label archiveStatusLabel;

    @FXML private TextField scenarioNameField;
    @FXML private TextField scenarioNotesField;
    @FXML private TableView<SavedScenario> scenarioTable;
    @FXML private TableColumn<SavedScenario, Integer> colScenarioId;
    @FXML private TableColumn<SavedScenario, String> colScenarioName;
    @FXML private TableColumn<SavedScenario, Integer> colScenarioCount;
    @FXML private TableColumn<SavedScenario, Double> colScenarioShy;
    @FXML private TableView<SimulationRun> runTable;
    @FXML private TableColumn<SimulationRun, Integer> colRunId;
    @FXML private TableColumn<SimulationRun, Integer> colRunGens;
    @FXML private TableColumn<SimulationRun, Integer> colRunEvac;
    @FXML private TableColumn<SimulationRun, Integer> colRunHits;

    @FXML private ComboBox<String> cityCombo;
    @FXML private CheckBox applyWeatherCheck;
    @FXML private ProgressBar weatherProgress;
    @FXML private TextArea jsonArea;

    private final SimulationEngine engine = new SimulationEngine();
    private final ScenarioDao scenarioDao = new ScenarioDao();
    private final RunDao runDao = new RunDao();
    private final WeatherService weatherService = new WeatherService();

    private Timeline timeline;
    private boolean playing;
    private boolean runSaved;
    private Operator operator;
    private Integer loadedScenarioId;
    private Runnable onLogout;

    @FXML
    public void initialize() {
        canvasHolder.setPadding(new Insets(6));
        simulationCanvas.widthProperty().bind(canvasHolder.widthProperty().subtract(12));
        simulationCanvas.heightProperty().bind(canvasHolder.heightProperty().subtract(12));
        simulationCanvas.widthProperty().addListener((o, a, b) -> render());
        simulationCanvas.heightProperty().addListener((o, a, b) -> render());

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

        colScenarioId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colScenarioName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colScenarioCount.setCellValueFactory(new PropertyValueFactory<>("studentCount"));
        colScenarioShy.setCellValueFactory(new PropertyValueFactory<>("shyPercent"));
        colRunId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRunGens.setCellValueFactory(new PropertyValueFactory<>("generations"));
        colRunEvac.setCellValueFactory(new PropertyValueFactory<>("evacuated"));
        colRunHits.setCellValueFactory(new PropertyValueFactory<>("collisions"));

        cityCombo.getItems().setAll("Dhaka", "Chittagong", "Sylhet");
        cityCombo.getSelectionModel().select("Dhaka");
        applyWeatherCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!n) {
                engine.setHazard(HazardConditions.CLEAR);
                updateStatsLabels();
            }
        });

        poolLabel.setText("Pool: " + SimulationExecutors.decisionWorkers() + " decision threads");

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        applyScenarioFromSliders();
        refreshArchiveTables();
        updateStatsLabels();
        render();
        updateButtonStates();
        Platform.runLater(() -> splitPane.setDividerPositions(0.62));
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
        operatorLabel.setText("Operator: " + operator.username());
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    public void bindResponsiveLayout(Scene scene) {
        titleLabel.styleProperty().bind(Bindings.concat(
                "-fx-font-size: ",
                Bindings.max(12.0, scene.widthProperty().divide(58)).asString("%.0f"),
                "px; -fx-font-weight: bold; -fx-text-fill: #f0f3f8;"));
        canvasHolder.minHeightProperty().bind(scene.heightProperty().multiply(0.72));
        canvasHolder.minWidthProperty().bind(scene.widthProperty().multiply(0.48));
    }

    @FXML
    private void onPlay() {
        if (engine.isFinished()) {
            return;
        }
        playing = true;
        restartTimeline();
        updateButtonStates();
        statusLabel.setText("Running on FX timeline; decisions use the thread pool.");
    }

    @FXML
    private void onPause() {
        playing = false;
        timeline.stop();
        updateButtonStates();
        statusLabel.setText("Paused");
    }

    @FXML
    private void onStep() {
        if (playing) {
            onPause();
        }
        if (!engine.isFinished()) {
            engine.step();
            afterStep();
        }
    }

    @FXML
    private void onReset() {
        onPause();
        applyScenarioFromSliders();
        runSaved = false;
        updateStatsLabels();
        render();
        updateButtonStates();
        statusLabel.setText("Reset");
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
        runSaved = false;
        updateStatsLabels();
        render();
        updateButtonStates();
    }

    @FXML
    private void onCreateScenario() {
        if (operator == null) {
            return;
        }
        SavedScenario scenario = fromForm();
        if (scenario.getName().isBlank()) {
            archiveStatusLabel.setText("Name is required.");
            return;
        }
        scenarioDao.insert(scenario);
        loadedScenarioId = scenario.getId();
        refreshArchiveTables();
        archiveStatusLabel.setText("Created scenario #" + scenario.getId());
    }

    @FXML
    private void onUpdateScenario() {
        SavedScenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            archiveStatusLabel.setText("Select a scenario to update.");
            return;
        }
        SavedScenario updated = fromForm();
        updated.setId(selected.getId());
        scenarioDao.update(updated);
        refreshArchiveTables();
        archiveStatusLabel.setText("Updated scenario #" + updated.getId());
    }

    @FXML
    private void onDeleteScenario() {
        SavedScenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            archiveStatusLabel.setText("Select a scenario to delete.");
            return;
        }
        scenarioDao.delete(selected.getId());
        if (loadedScenarioId != null && loadedScenarioId == selected.getId()) {
            loadedScenarioId = null;
        }
        refreshArchiveTables();
        archiveStatusLabel.setText("Deleted scenario #" + selected.getId());
    }

    @FXML
    private void onLoadScenario() {
        SavedScenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            archiveStatusLabel.setText("Select a scenario to load.");
            return;
        }
        onPause();
        studentCountSlider.setValue(selected.getStudentCount());
        shyPercentSlider.setValue(selected.getShyPercent());
        scenarioNameField.setText(selected.getName());
        scenarioNotesField.setText(selected.getNotes());
        loadedScenarioId = selected.getId();
        applyScenarioFromSliders();
        runSaved = false;
        updateStatsLabels();
        render();
        updateButtonStates();
        archiveStatusLabel.setText("Loaded scenario #" + selected.getId());
    }

    @FXML
    private void onDeleteRun() {
        SimulationRun selected = runTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            archiveStatusLabel.setText("Select a run to delete.");
            return;
        }
        runDao.delete(selected.getId());
        refreshArchiveTables();
        archiveStatusLabel.setText("Deleted run #" + selected.getId());
    }

    @FXML
    private void onFetchWeather() {
        String city = cityCombo.getValue() == null ? "Dhaka" : cityCombo.getValue();
        weatherProgress.setVisible(true);
        fetchWeatherButton.setDisable(true);
        statusLabel.setText("Fetching weather JSON on IO thread…");
        SimulationExecutors.io().submit(() -> {
            try {
                HazardConditions hazard = weatherService.fetch(city);
                String json = weatherService.getLastRawJson();
                Platform.runLater(() -> {
                    jsonArea.setText(json);
                    weatherLabel.setText(hazard.getSummary()
                            + String.format("  (hesitation +%.0f%%)", hazard.getHesitationExtra() * 100));
                    if (applyWeatherCheck.isSelected()) {
                        engine.setHazard(hazard);
                    } else {
                        engine.setHazard(HazardConditions.CLEAR);
                    }
                    updateStatsLabels();
                    statusLabel.setText("Weather parsed with Gson.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    weatherLabel.setText("Fetch failed: " + ex.getMessage());
                    statusLabel.setText("HTTP request failed");
                });
            } finally {
                Platform.runLater(() -> {
                    weatherProgress.setVisible(false);
                    fetchWeatherButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void onLogout() {
        onPause();
        if (onLogout != null) {
            onLogout.run();
        }
    }

    @FXML
    private void onExit() {
        Window window = simulationCanvas.getScene() == null ? null : simulationCanvas.getScene().getWindow();
        if (window != null) {
            window.hide();
        }
        Platform.exit();
    }

    @FXML
    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Assignment topics");
        alert.setHeaderText("Mapped into this simulator");
        alert.setContentText("""
                OOP: Occupant abstract class, MovementBehavior interface, AbstractMovementBehavior.
                JavaFX: StackPane login, GridPane form, PasswordField, BorderPane, SplitPane, TabPane, TableView.
                Responsive: canvas bound to pane size; title font bound to scene width.
                Concurrency: fixed thread pool for student decisions + IO pool for HTTP.
                SQLite: operators / scenarios / simulation_runs with foreign keys; full scenario CRUD.
                Networking: HttpClient GET Open-Meteo; Gson parses JSON into hazard rules.
                """);
        alert.showAndWait();
    }

    private SavedScenario fromForm() {
        SavedScenario scenario = new SavedScenario();
        scenario.setName(scenarioNameField.getText() == null ? "" : scenarioNameField.getText().trim());
        scenario.setNotes(scenarioNotesField.getText());
        scenario.setStudentCount((int) Math.round(studentCountSlider.getValue()));
        scenario.setShyPercent(shyPercentSlider.getValue());
        scenario.setOperatorId(operator == null ? 1 : operator.id());
        return scenario;
    }

    private void refreshArchiveTables() {
        scenarioTable.getItems().setAll(scenarioDao.findAll());
        runTable.getItems().setAll(runDao.findRecent(20));
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
            afterStep();
            if (engine.isFinished()) {
                onPause();
            }
        }));
        timeline.play();
    }

    private void afterStep() {
        updateStatsLabels();
        render();
        updateButtonStates();
        if (engine.isFinished() && !runSaved) {
            persistFinishedRun();
        }
    }

    private void persistFinishedRun() {
        if (operator == null) {
            return;
        }
        SimulationStats stats = engine.getStats();
        SimulationRun run = new SimulationRun();
        run.setScenarioId(loadedScenarioId);
        run.setOperatorId(operator.id());
        run.setGenerations(stats.getGeneration());
        run.setEvacuated(stats.getEvacuated());
        run.setCollisions(stats.getCollisions());
        run.setWeatherSummary(engine.getHazard().getSummary());
        run.setFinished(true);
        runSaved = true;
        SimulationExecutors.io().submit(() -> {
            runDao.insert(run);
            Platform.runLater(() -> {
                refreshArchiveTables();
                statusLabel.setText("Run saved to SQLite.");
            });
        });
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
        exitStatusLabel.setText("Exits: " + s.getExitStatusSummary());
        if (s.getEvacuationTime() >= 0) {
            evacTimeLabel.setText("Evacuation Time: " + s.getEvacuationTime() + " gens");
        } else {
            evacTimeLabel.setText("Evacuation Time: —");
        }
        hazardStatusLabel.setText("Hazard: " + engine.getHazard().getSummary());
    }

    private void render() {
        GraphicsContext gc = simulationCanvas.getGraphicsContext2D();
        double width = simulationCanvas.getWidth();
        double height = simulationCanvas.getHeight();
        if (width <= 1 || height <= 1) {
            return;
        }
        gc.setFill(Color.web("#12151c"));
        gc.fillRect(0, 0, width, height);

        Room room = engine.getRoom();
        double cell = Math.min(width / Room.COLS, height / Room.ROWS);
        double offsetX = (width - cell * Room.COLS) / 2.0;
        double offsetY = (height - cell * Room.ROWS) / 2.0;

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

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(8, cell * 0.72)));
        for (Exit exit : room.getExits()) {
            Color color = switch (exit.getStatus()) {
                case OPEN -> COLOR_EXIT_OPEN;
                case CONGESTED -> COLOR_EXIT_BUSY;
                case BLOCKED -> COLOR_EXIT_BLOCKED;
            };
            gc.setFill(color);
            for (Position p : exit.getCells()) {
                double x = offsetX + p.col() * cell;
                double y = offsetY + p.row() * cell;
                gc.fillRect(x, y, cell, cell);
                gc.setStroke(Color.web("#ecfdf5"));
                gc.setLineWidth(Math.max(1.0, cell * 0.12));
                gc.strokeRect(x + 0.5, y + 0.5, cell - 1, cell - 1);
            }
            Position labelCell = exit.getCells().get(exit.getCells().size() / 2);
            gc.setFill(Color.web("#052e16"));
            gc.fillText("E" + (exit.getId() + 1),
                    offsetX + labelCell.col() * cell + cell * 0.08,
                    offsetY + labelCell.row() * cell + cell * 0.78);
        }

        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(0.5);
        for (int r = 0; r <= Room.ROWS; r++) {
            double y = offsetY + r * cell;
            gc.strokeLine(offsetX, y, offsetX + Room.COLS * cell, y);
        }
        for (int c = 0; c <= Room.COLS; c++) {
            double x = offsetX + c * cell;
            gc.strokeLine(x, offsetY, x, offsetY + Room.ROWS * cell);
        }

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
