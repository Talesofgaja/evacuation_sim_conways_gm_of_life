package com.evacsim.controller;

import com.evacsim.db.Operator;
import com.evacsim.db.OperatorDao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final OperatorDao operatorDao = new OperatorDao();
    private Consumer<Operator> onSuccess;

    public void setOnSuccess(Consumer<Operator> onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Enter username and password.");
            return;
        }
        operatorDao.authenticate(user, pass).ifPresentOrElse(operator -> {
            errorLabel.setText("");
            if (onSuccess != null) {
                onSuccess.accept(operator);
            }
        }, () -> errorLabel.setText("Invalid credentials (check SQLite operators table)."));
    }
}
