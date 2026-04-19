package edu.uic.cs342.controller;

import edu.uic.cs342.SceneManager;
import edu.uic.cs342.http.ClientThread;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

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

    public void setSceneManager(SceneManager sm) { this.sceneManager = sm; }
    public void setClientThread(ClientThread ct) { this.client       = ct; }

    // ── Methods ───────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        this.hideLabel(this.loginErrorLabel);
        this.hideLabel(this.signupErrorLabel);
        this.hideLabel(this.signupSuccessLabel);
    }

    public void reset() {
        this.loginUsernameField.clear();
        this.loginPasswordField.clear();
        this.signupUsernameField.clear();
        this.signupPasswordField.clear();
        this.signupConfirmField.clear();
        this.hideLabel(this.loginErrorLabel);
        this.hideLabel(this.signupErrorLabel);
        this.hideLabel(this.signupSuccessLabel);
        this.tabPane.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleLogin() {
        String username = this.loginUsernameField.getText().trim();
        String password = this.loginPasswordField.getText();

        if (username.isBlank()) { this.showError(this.loginErrorLabel, "Username is required."); return; }
        if (password.isBlank()) { this.showError(this.loginErrorLabel, "Password is required.");  return; }

        this.setLoading(this.loginButton, true, "Login");
        this.hideLabel(this.loginErrorLabel);

        this.client.login(username, password,
            json -> {
                this.setLoading(this.loginButton, false, "Login");
                if (json.path("success").asBoolean()) {
                    this.sceneManager.showLobby(json.path("username").asText());
                } else {
                    this.showError(this.loginErrorLabel, "Login failed.");
                }
            },
            err -> {
                this.setLoading(this.loginButton, false, "Login");
                this.showError(this.loginErrorLabel, err);
            });
    }

    @FXML
    private void handleSignup() {
        String username = this.signupUsernameField.getText().trim();
        String password = this.signupPasswordField.getText();
        String confirm  = this.signupConfirmField.getText();

        this.hideLabel(this.signupErrorLabel);
        this.hideLabel(this.signupSuccessLabel);

        if (username.isBlank())        { this.showError(this.signupErrorLabel, "Username is required.");             return; }
        if (username.length() < 3)     { this.showError(this.signupErrorLabel, "Username must be >= 3 characters."); return; }
        if (password.length() < 6)     { this.showError(this.signupErrorLabel, "Password must be >= 6 characters."); return; }
        if (!password.equals(confirm)) { this.showError(this.signupErrorLabel, "Passwords do not match.");           return; }

        this.setLoading(this.signupButton, true, "Sign Up");

        this.client.signup(username, password,
            json -> {
                this.setLoading(this.signupButton, false, "Sign Up");
                this.signupUsernameField.clear();
                this.signupPasswordField.clear();
                this.signupConfirmField.clear();
                this.showLabel(this.signupSuccessLabel, "Account created! Please log in.", "-fx-text-fill: #4ade80;");
                this.tabPane.getSelectionModel().selectFirst();
                this.loginUsernameField.setText(username);
            },
            err -> {
                this.setLoading(this.signupButton, false, "Sign Up");
                this.showError(this.signupErrorLabel, err);
            });
    }

    private void showError(Label label, String msg) {
        this.showLabel(label, msg, "-fx-text-fill: #f87171;");
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
