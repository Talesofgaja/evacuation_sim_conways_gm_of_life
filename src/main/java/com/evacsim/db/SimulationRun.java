package com.evacsim.db;

public class SimulationRun {

    private int id;
    private Integer scenarioId;
    private int operatorId;
    private int generations;
    private int evacuated;
    private int collisions;
    private String weatherSummary;
    private boolean finished;
    private String createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Integer scenarioId) {
        this.scenarioId = scenarioId;
    }

    public int getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(int operatorId) {
        this.operatorId = operatorId;
    }

    public int getGenerations() {
        return generations;
    }

    public void setGenerations(int generations) {
        this.generations = generations;
    }

    public int getEvacuated() {
        return evacuated;
    }

    public void setEvacuated(int evacuated) {
        this.evacuated = evacuated;
    }

    public int getCollisions() {
        return collisions;
    }

    public void setCollisions(int collisions) {
        this.collisions = collisions;
    }

    public String getWeatherSummary() {
        return weatherSummary;
    }

    public void setWeatherSummary(String weatherSummary) {
        this.weatherSummary = weatherSummary;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
