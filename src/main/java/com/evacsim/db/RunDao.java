package com.evacsim.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class RunDao {

    public void insert(SimulationRun run) {
        try (var ps = Database.getConnection().prepareStatement(
                """
                INSERT INTO simulation_runs(
                    scenario_id, operator_id, generations, evacuated, collisions,
                    weather_summary, finished)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            if (run.getScenarioId() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, run.getScenarioId());
            }
            ps.setInt(2, run.getOperatorId());
            ps.setInt(3, run.getGenerations());
            ps.setInt(4, run.getEvacuated());
            ps.setInt(5, run.getCollisions());
            ps.setString(6, run.getWeatherSummary());
            ps.setInt(7, run.isFinished() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<SimulationRun> findRecent(int limit) {
        List<SimulationRun> list = new ArrayList<>();
        try (var ps = Database.getConnection().prepareStatement(
                """
                SELECT id, scenario_id, operator_id, generations, evacuated, collisions,
                       weather_summary, finished, created_at
                FROM simulation_runs
                ORDER BY id DESC
                LIMIT ?
                """)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SimulationRun run = new SimulationRun();
                run.setId(rs.getInt("id"));
                int scenarioId = rs.getInt("scenario_id");
                run.setScenarioId(rs.wasNull() ? null : scenarioId);
                run.setOperatorId(rs.getInt("operator_id"));
                run.setGenerations(rs.getInt("generations"));
                run.setEvacuated(rs.getInt("evacuated"));
                run.setCollisions(rs.getInt("collisions"));
                run.setWeatherSummary(rs.getString("weather_summary"));
                run.setFinished(rs.getInt("finished") == 1);
                run.setCreatedAt(rs.getString("created_at"));
                list.add(run);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return list;
    }

    public void delete(int id) {
        try (var ps = Database.getConnection().prepareStatement(
                "DELETE FROM simulation_runs WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
