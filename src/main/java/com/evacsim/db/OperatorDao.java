package com.evacsim.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class OperatorDao {

    public Optional<Operator> authenticate(String username, String password) {
        String hash = Database.hashPassword(password);
        try (var ps = Database.getConnection().prepareStatement(
                "SELECT id, username FROM operators WHERE username = ? AND password_hash = ?")) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new Operator(rs.getInt("id"), rs.getString("username")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
