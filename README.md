# Crowd Evacuation Simulator

JavaFX + FXML cellular-automaton classroom evacuation (Maven, Java 17+).

Students are colored blocks on a grid of ~128 seats in 4 bench columns. Each generation they choose a next cell based on distance-to-exit, obstacles, crowd density, and personality (**Shy** vs **Angry**). Conflicts, stuns, and exit blockages emerge from the rules.

## Run

```powershell
$env:JAVA_HOME = "C:\Users\agomo\jdk-17"   # if needed
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean javafx:run
```

## Package structure

```
com.evacsim
├── Main.java
├── controller/SimulationController.java   UI ↔ engine glue
├── model/                                 Room, Cell, Student, Exit, …
├── engine/SimulationEngine.java           generation step + conflicts
├── engine/behavior/                       ShyBehavior, AngryBehavior
└── stats/SimulationStats.java
```

## Controls

Play · Pause · Step · Reset · Speed · Students (60–80) · Shy% · Randomize Scenario

## Legend

| Color | Meaning |
|-------|---------|
| Blue | Shy |
| Orange | Angry |
| Purple | Stunned |
| Green / Yellow / Red | Exit open / busy / blocked |
