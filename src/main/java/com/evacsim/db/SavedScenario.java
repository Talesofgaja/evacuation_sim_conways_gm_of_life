package com.evacsim.db;

public class SavedScenario {

    private int id;
    private String name;
    private int studentCount;
    private double shyPercent;
    private String notes;
    private int operatorId;

    public SavedScenario() {
    }

    public SavedScenario(int id, String name, int studentCount, double shyPercent,
                         String notes, int operatorId) {
        this.id = id;
        this.name = name;
        this.studentCount = studentCount;
        this.shyPercent = shyPercent;
        this.notes = notes;
        this.operatorId = operatorId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public double getShyPercent() {
        return shyPercent;
    }

    public void setShyPercent(double shyPercent) {
        this.shyPercent = shyPercent;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(int operatorId) {
        this.operatorId = operatorId;
    }
}
