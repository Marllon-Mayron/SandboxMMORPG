package com.sandbox.client.screens;

import com.sandbox.client.SandboxClient;
import com.sandbox.client.screens.auth.AdminModeSelectorScreen;
import com.sandbox.client.screens.auth.LoginScreen;
import com.sandbox.client.screens.auth.RegisterScreen;
import com.sandbox.client.screens.game.GameScreen;
import com.common.sandbox.model.player.Player;

import java.util.Map;

public class ScreenManager {
    private final SandboxClient game;

    public ScreenManager(SandboxClient game) {
        this.game = game;
    }

    public void showAdminSelector() {
        AdminModeSelectorScreen screen = new AdminModeSelectorScreen(game);
        game.setScreen(screen);
    }

    public void showLogin(boolean adminMode) {
        LoginScreen screen = new LoginScreen(game, adminMode);
        game.setScreen(screen);
    }

    public void showRegister(boolean adminMode) {
        RegisterScreen screen = new RegisterScreen(game, adminMode);
        game.setScreen(screen);
    }

    public void showGame(Player player, boolean adminMode, Map<String, Player> nearbyPlayers) {
        GameScreen gameScreen = new GameScreen(game, player, adminMode, nearbyPlayers);
        game.setScreen(gameScreen);
    }

    public void showMapEditor() {
        com.sandbox.client.editor.MapEditorScreen editorScreen =
                new com.sandbox.client.editor.MapEditorScreen(game);
        game.setScreen(editorScreen);
    }
}