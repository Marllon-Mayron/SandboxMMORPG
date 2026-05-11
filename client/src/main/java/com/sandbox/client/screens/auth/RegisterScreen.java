package com.sandbox.client.screens.auth;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.screens.AbstractScreen;
import com.common.sandbox.network.packets.RegisterResponse;

public class RegisterScreen extends AbstractScreen {
    private final boolean adminMode;
    private TextField usernameField;
    private TextField emailField;
    private TextField passwordField;
    private TextField confirmPasswordField;
    private Label errorLabel;

    public RegisterScreen(SandboxClient game, boolean adminMode) {
        super(game);
        this.adminMode = adminMode;
    }

    @Override
    protected void initUI() {
        Table table = new Table();
        centerTable(table);

        Label titleLabel = new Label("CREATE ACCOUNT", skin, "default");
        titleLabel.setFontScale(1.8f);
        table.add(titleLabel).padBottom(40);
        table.row();

        Table formTable = new Table();

        // Username
        formTable.add(new Label("Username:", skin)).left().padRight(10).width(120);
        usernameField = new TextField("", skin);
        formTable.add(usernameField).width(250).padBottom(15);
        formTable.row();

        // Email
        formTable.add(new Label("Email:", skin)).left().padRight(10).width(120);
        emailField = new TextField("", skin);
        formTable.add(emailField).width(250).padBottom(15);
        formTable.row();

        // Password
        formTable.add(new Label("Password:", skin)).left().padRight(10).width(120);
        passwordField = new TextField("", skin);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        formTable.add(passwordField).width(250).padBottom(15);
        formTable.row();

        // Confirm Password
        formTable.add(new Label("Confirm Password:", skin)).left().padRight(10).width(120);
        confirmPasswordField = new TextField("", skin);
        confirmPasswordField.setPasswordMode(true);
        confirmPasswordField.setPasswordCharacter('*');
        formTable.add(confirmPasswordField).width(250).padBottom(25);
        formTable.row();

        // Buttons
        Table buttonTable = new Table();

        TextButton registerButton = new TextButton("REGISTER", skin, "default");
        registerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleRegister();
            }
        });
        buttonTable.add(registerButton).width(120).padRight(10);

        TextButton backButton = new TextButton("BACK TO LOGIN", skin, "default");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getScreenManager().showLogin(adminMode);
            }
        });
        buttonTable.add(backButton).width(120);

        formTable.add(buttonTable).colspan(2);

        table.add(formTable);

        errorLabel = new Label("", skin, "error");
        table.add(errorLabel).padTop(20);
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill all fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            return;
        }

        if (password.length() < 4) {
            errorLabel.setText("Password must be at least 4 characters");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            errorLabel.setText("Please enter a valid email");
            return;
        }

        errorLabel.setText("Registering...");
        errorLabel.setColor(1, 1, 0, 1);

        game.getNetworkClient().setRegisterCallback(this::onRegisterResponse);
        game.getNetworkClient().sendRegister(username, email, password);
    }

    private void onRegisterResponse(RegisterResponse response) {
        Gdx.app.postRunnable(() -> {
            if (response.success) {
                errorLabel.setText(response.message);
                errorLabel.setColor(0, 1, 0, 1);
                errorLabel.setStyle(skin.get("success", Label.LabelStyle.class));

                // Return to login after 2 seconds
                Thread.startVirtualThread(() -> {
                    try {
                        Thread.sleep(2000);
                        Gdx.app.postRunnable(() -> {
                            game.getScreenManager().showLogin(adminMode);
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } else {
                errorLabel.setText(response.message);
                errorLabel.setColor(1, 0, 0, 1);
                errorLabel.setStyle(skin.get("error", Label.LabelStyle.class));
            }
        });
    }

    @Override
    public void render(float delta) {
        float r = adminMode ? 0.08f : 0.1f;
        float g = adminMode ? 0.05f : 0.1f;
        float b = adminMode ? 0.12f : 0.1f;
        Gdx.gl.glClearColor(r, g, b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render(delta);
    }
}