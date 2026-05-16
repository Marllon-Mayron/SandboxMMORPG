package com.sandbox.client.screens.auth;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.sandbox.client.NetworkClient;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.editor.MapEditorScreen;
import com.sandbox.client.screens.AbstractScreen;
import com.common.sandbox.network.packets.auth.LoginResponse;

public class LoginScreen extends AbstractScreen {
    private final boolean adminMode;
    private TextField usernameField;
    private TextField passwordField;
    private TextField hostField;
    private TextField portField;
    private Label errorLabel;
    private Label modeIndicator;

    // Admin credentials (only user marllon can access admin mode)
    private static final String ADMIN_USERNAME = "marllon";
    private static final String ADMIN_PASSWORD = "123456";

    // Configurações padrão
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;

    public LoginScreen(SandboxClient game, boolean adminMode) {
        super(game);
        this.adminMode = adminMode;
    }

    @Override
    protected void initUI() {
        Table table = new Table();
        centerTable(table);

        // Title
        String title = adminMode ? "ADMIN LOGIN" : "LOGIN";
        Label titleLabel = new Label(title, skin, adminMode ? "title" : "default");
        titleLabel.setFontScale(1.8f);
        table.add(titleLabel).padBottom(40);
        table.row();

        if (adminMode) {
            modeIndicator = new Label("Admin Mode - Login with admin credentials", skin, "title");
            modeIndicator.setFontScale(0.9f);
            table.add(modeIndicator).padBottom(20);
            table.row();
        }

        Table formTable = new Table();

        // Host
        formTable.add(new Label("Server Host:", skin)).left().padRight(10).width(100);
        hostField = new TextField(DEFAULT_HOST, skin);
        hostField.setMessageText("ex: localhost or 192.168.1.100");
        formTable.add(hostField).width(250).padBottom(10);
        formTable.row();

        // Porta
        formTable.add(new Label("Server Port:", skin)).left().padRight(10).width(100);
        portField = new TextField(String.valueOf(DEFAULT_PORT), skin);
        portField.setMessageText("ex: 8080");
        formTable.add(portField).width(250).padBottom(15);
        formTable.row();

        // Username
        formTable.add(new Label("Username:", skin)).left().padRight(10).width(100);
        usernameField = new TextField("", skin);
        formTable.add(usernameField).width(250).padBottom(15);
        formTable.row();

        // Password
        formTable.add(new Label("Password:", skin)).left().padRight(10).width(100);
        passwordField = new TextField("", skin);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        formTable.add(passwordField).width(250).padBottom(25);
        formTable.row();

        // Buttons
        Table buttonTable = new Table();

        String buttonStyle = adminMode ? "admin" : "default";
        TextButton loginButton = new TextButton("LOGIN", skin, buttonStyle);
        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleLogin();
            }
        });
        buttonTable.add(loginButton).width(120).padRight(10);

        TextButton registerButton = new TextButton("REGISTER", skin, "default");
        registerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getScreenManager().showRegister(adminMode);
            }
        });
        buttonTable.add(registerButton).width(120);

        formTable.add(buttonTable).colspan(2);
        formTable.row();

        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) {
            formTable.row();
            TextButton backButton = new TextButton("BACK TO MODE SELECTOR", skin, "default");
            backButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.getScreenManager().showAdminSelector();
                }
            });
            formTable.add(backButton).colspan(2).width(250).padTop(15);
        }

        table.add(formTable);

        // Error label
        errorLabel = new Label("", skin, "error");
        table.add(errorLabel).padTop(20);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill all fields");
            return;
        }

        if (host.isEmpty()) {
            host = DEFAULT_HOST;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid port number (1-65535)");
            return;
        }


        if (adminMode) {
            if (!ADMIN_USERNAME.equals(username) || !ADMIN_PASSWORD.equals(password)) {
                errorLabel.setText("CREDENCIAIS INVALIDAS PARA ACESSAR O MODO ADM");
                errorLabel.setColor(1, 0, 0, 1);
                return;
            }
            errorLabel.setText("");
        }

        errorLabel.setText("Conectando em: " + host + ":" + port + "...");
        errorLabel.setColor(1, 1, 0, 1);

        reconnectToServer(host, port, username, password);
    }

    private void reconnectToServer(String host, int port, String username, String password) {
        game.getNetworkClient().disconnect();

        NetworkClient newClient = new NetworkClient(host, port);
        game.setNetworkClient(newClient);
        newClient.connect();

        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(500);
                Gdx.app.postRunnable(() -> {
                    game.getNetworkClient().setLoginCallback(this::onLoginResponse);
                    game.getNetworkClient().sendLogin(username, password);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Gdx.app.postRunnable(() -> {
                    errorLabel.setText("Connection error: " + e.getMessage());
                    errorLabel.setColor(1, 0, 0, 1);
                });
            }
        });
    }

    private void onLoginResponse(LoginResponse response) {
        Gdx.app.postRunnable(() -> {
            if (response.success) {
                logger.info("Login successful: {}", response.player.getUsername());

                if (adminMode) {
                    logger.info("Admin mode - Opening Map Editor");
                    MapEditorScreen editorScreen = new MapEditorScreen(game);
                    game.setScreen(editorScreen);
                } else {
                    // Passar nearbyPlayers para o jogo
                    game.startGame(response.player, false, response.nearbyPlayers);
                }
            } else {
                errorLabel.setText(response.message);
                errorLabel.setColor(1, 0, 0, 1);
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