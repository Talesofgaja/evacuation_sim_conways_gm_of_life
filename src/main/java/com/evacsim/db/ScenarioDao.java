package com.evacsim.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ScenarioDao {

    public List<SavedScenario> findAll() {
        List<SavedScenario> list = new ArrayList<>();
        try (var ps = Database.getConnection().prepareStatement(
                "SELECT id, name, student_count, shy_percent, notes, operator_id FROM scenarios ORDER BY id DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return list;
    }

    public SavedScenario insert(SavedScenario scenario) {
        try (var ps = Database.getConnection().prepareStatement(
                """
                INSERT INTO scenarios(name, student_count, shy_percent, notes, operator_id)
                VALUES (?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, scenario.getName());
            ps.setInt(2, scenario.getStudentCount());
            ps.setDouble(3, scenario.getShyPercent());
            ps.setString(4, scenario.getNotes());
            ps.setInt(5, scenario.getOperatorId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                scenario.setId(keys.getInt(1));
            }
            return scenario;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void update(SavedScenario scenario) {
        try (var ps = Database.getConnection().prepareStatement(
                """
                UPDATE scenarios
                SET name = ?, student_count = ?, shy_percent = ?, notes = ?
                WHERE id = ?
                """)) {
            ps.setString(1, scenario.getName());
            ps.setInt(2, scenario.getStudentCount());
            ps.setDouble(3, scenario.getShyPercent());
            ps.setString(4, scenario.getNotes());
            ps.setInt(5, scenario.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void delete(int id) {
        try (var ps = Database.getConnection().prepareStatement("DELETE FROM scenarios WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SavedScenario map(ResultSet rs) throws SQLException {
        return new SavedScenario(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("student_count"),
                rs.getDouble("shy_percent"),
                rs.getString("notes"),
                rs.getInt("operator_id"));
    }
}
