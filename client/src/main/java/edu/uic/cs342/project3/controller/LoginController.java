package edu.uic.cs342.project3.controller;

import edu.uic.cs342.project3.SceneManager;
import edu.uic.cs342.project3.http.ClientThread;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

/**
 * Controller for login.fxml.
 * Calls ClientThread directly for all network operations.
 */
public class LoginController {

    // ── Fields ────────────────────────────────────────────────────────────────

    @FXML private TabPane       tabPane;
    @FXML private TextField     loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Label         loginErrorLabel;
    @FXML private Button        loginButton;
    @FXML private TextField     signupUsernameField;
    @FXML private PasswordField signupPasswordField;
    @FXML private PasswordField signupConfirmField;
    @FXML private Label         signupErrorLabel;
    @FXML private Label         signupSuccessLabel;
    @FXML private Button        signupButton;

    private SceneManager sceneManager;
    private ClientThread client;

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setSceneManager(SceneManager sm) { this.sceneManager = sm;     }
    public void setClientThread(ClientThread ct) { this.client       = ct;     }

    // ── Methods ───────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        hideLabel(loginErrorLabel);
        hideLabel(signupErrorLabel);
        hideLabel(signupSuccessLabel);
    }

    public void reset() {
        loginUsernameField.clear();
        loginPasswordField.clear();
        signupUsernameField.clear();
        signupPasswordField.clear();
        signupConfirmField.clear();
        hideLabel(loginErrorLabel);
        hideLabel(signupErrorLabel);
        hideLabel(signupSuccessLabel);
        tabPane.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isBlank()) { showError(loginErrorLabel, "Username is required."); return; }
        if (password.isBlank()) { showError(loginErrorLabel, "Password is required.");  return; }

        setLoading(loginButton, true, "Login");
        hideLabel(loginErrorLabel);

        client.login(username, password,
            json -> {
                setLoading(loginButton, false, "Login");
                if (json.path("success").asBoolean()) {
                    sceneManager.showLobby(json.path("username").asText());
                } else {
                    showError(loginErrorLabel, "Login failed.");
                }
            },
            err -> {
                setLoading(loginButton, false, "Login");
                showError(loginErrorLabel, err);
            });
    }

    @FXML
    private void handleSignup() {
        String username = signupUsernameField.getText().trim();
        String password = signupPasswordField.getText();
        String confirm  = signupConfirmField.getText();

        hideLabel(signupErrorLabel);
        hideLabel(signupSuccessLabel);

        if (username.isBlank())        { showError(signupErrorLabel, "Username is required.");             return; }
        if (username.length() < 3)     { showError(signupErrorLabel, "Username must be >= 3 characters."); return; }
        if (password.length() < 6)     { showError(signupErrorLabel, "Password must be >= 6 characters."); return; }
        if (!password.equals(confirm)) { showError(signupErrorLabel, "Passwords do not match.");           return; }

        setLoading(signupButton, true, "Sign Up");

        client.signup(username, password,
            json -> {
                setLoading(signupButton, false, "Sign Up");
                signupUsernameField.clear();
                signupPasswordField.clear();
                signupConfirmField.clear();
                showLabel(signupSuccessLabel, "Account created! Please log in.", "-fx-text-fill: #4ade80;");
                tabPane.getSelectionModel().selectFirst();
                loginUsernameField.setText(username);
            },
            err -> {
                setLoading(signupButton, false, "Sign Up");
                showError(signupErrorLabel, err);
            });
    }

    private void showError(Label label, String msg) {
        showLabel(label, msg, "-fx-text-fill: #f87171;");
    }

    private void showLabel(Label label, String msg, String style) {
        label.setText(msg);
        label.setStyle(style);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideLabel(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void setLoading(Button btn, boolean loading, String idleText) {
        btn.setDisable(loading);
        btn.setText(loading ? "Please wait..." : idleText);
    }
}
