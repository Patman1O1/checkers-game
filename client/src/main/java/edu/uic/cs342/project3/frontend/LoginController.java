package edu.uic.cs342.project3.frontend;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class LoginController {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private SceneManager sceneManager;

    private Alert error;

    @FXML
    private TabPane tabPane;

    @FXML
    private TextField loginUsername;

    @FXML
    private PasswordField loginPassword;

    @FXML
    private Button loginButton;

    //@FXML
    //private Label loginError;

    @FXML
    private TextField signupUsername;

    @FXML
    private PasswordField signupPassword;

    @FXML
    private PasswordField signupConfirm;

    @FXML
    private Button signupButton;

    //@FXML
    //private Label signupError;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public LoginController() throws IOException {
        this.sceneManager = SceneManager.getInstance();
        this.error = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
    }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    private void setLoading(Button button, boolean loading, String text) {
        button.setDisable(loading);
        button.setText(text);
    }

    private void showError(String message) {
        this.error.setContentText(message);
        this.error.show();
    }

    @FXML
    private void handleLogin() {
        // Extract username and password from fields
        String username = this.loginUsername.getText().trim();
        String password = this.loginPassword.getText();

        // Ensure fields are not empty
        if (username.isEmpty() || password.isEmpty()) {
            this.showError("Please fill in all fields.");
            return;
        }

        this.setLoading(this.loginButton, true, "Logging in...");
        this.error.hide();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Connect to server if not already connected
                //connectIfNeeded();
                //NetworkService.get().login(username, password);
                return null;
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            //SessionStore.username = username;
            this.sceneManager.showLobbyScene();
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(loginButton, false, "Login");
            this.showError(task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    @FXML
    private void handleSignup() {
        String username = this.signupUsername.getText().trim();
        String password = this.signupPassword.getText();
        String confirm = this.signupConfirm.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            this.showError("Please fill in all fields.");
            return;
        }
        if (!password.equals(confirm)) {
            this.showError("Passwords do not match.");
            return;
        }
        if (password.length() < 6) {
            this.showError("Password must be at least 6 characters.");
            return;
        }

        this.setLoading(this.signupButton, true, "Creating Account...");
        this.error.hide();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                //connectIfNeeded();
                //NetworkService.get().signup(user, pass);
                return null;
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            this.setLoading(this.signupButton, false, "Sign Up");

            // Clear form and switch to login tab — mirrors React success flow
            this.signupUsername.clear();
            this.signupPassword.clear();
            this.signupConfirm.clear();
            this.tabPane.getSelectionModel().select(0);
            this.showError("");
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            this.setLoading(this.signupButton, false, "Sign Up");
            this.showError(task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    @FXML
    public void initialize() {
        this.loginPassword.setOnAction(event -> handleLogin());
        this.signupConfirm.setOnAction(event -> handleSignup());
    }
}
