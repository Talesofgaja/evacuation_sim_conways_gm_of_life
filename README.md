# Crowd Evacuation Simulator

JavaFX classroom evacuation (Maven, Java 17+). Students are colored cells on a ~128-seat grid.
Each generation they pick a next cell from distance-to-exit, obstacles, density, personality
(**Shy** vs **Angry**), and optional live weather hazard. Conflicts can stun agents and jam exits.

## Run

```powershell
$env:JAVA_HOME = "C:\Users\agomo\jdk-17"   # if needed
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean javafx:run
```

Login (SQLite `operators` table):

- `admin` / `admin123`
- `operator` / `pass123`

Window stays compact (~900×620). Resize it: the grid scales so **E1 / E2** (bottom) and **E3** (left wall) stay on screen.

SQLite file: `evacsim.db` in the project working directory.

## Assignment mapping (demo script)

| Topic | Where to show |
| --- | --- |
| GitHub / commits | Repository history from idea date |
| OOP | `Occupant` abstract class, `MovementBehavior` interface, `AbstractMovementBehavior`, `Student` |
| JavaFX panes / controls | Login `StackPane` + `GridPane` + `PasswordField`; main `BorderPane`, `SplitPane`, `TabPane`, `TableView`, `ComboBox`, `MenuBar`, `ToolBar` |
| Responsive layout | Canvas bound to holder size; title font bound to scene width; `SplitPane` height bound to window |
| Concurrency | `SimulationExecutors` thread pool; `SimulationEngine.decideInParallel`; weather fetch on IO pool |
| SQLite + FK | `operators` 1—N `scenarios` 1—N `simulation_runs` in `Database.java` |
| CRUD | Archive tab: Create / Read (table) / Update / Delete scenarios; delete runs; auto-insert run when a drill finishes |
| HTTP + JSON | Weather tab: `HttpClient` GET Open-Meteo; Gson parses `current` into `HazardConditions` |

Keep the video ≤ 5 minutes: login → resize window / point at E1–E3 → Play → Archive CRUD → Weather fetch → Help menu topic list → GitHub commits.

## Package structure

```
com.evacsim
├── Main.java
├── concurrent/SimulationExecutors.java
├── controller/          login + simulation UI
├── db/                  SQLite schema + DAOs
├── engine/              generation step + conflict resolution
├── engine/behavior/     Shy / Angry strategies
├── model/               Occupant, Room, Exit, HazardConditions, …
├── net/WeatherService.java
└── stats/SimulationStats.java
```
