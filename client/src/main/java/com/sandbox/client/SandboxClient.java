package com.sandbox.client;

import com.badlogic.gdx.Game;
import com.sandbox.client.screens.ScreenManager;
import com.common.sandbox.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SandboxClient extends Game {
    private static final Logger logger = LoggerFactory.getLogger(SandboxClient.class);

    private NetworkClient networkClient;
    private ScreenManager screenManager;
    private boolean isProduction = false;

    @Override
    public void create() {
        logger.info("Starting Sandbox Client");

        networkClient = new NetworkClient("localhost", 8080);
        networkClient.connect();

        screenManager = new ScreenManager(this);

        // Show admin mode selector only in development
        if (!isProduction) {
            screenManager.showAdminSelector();
        } else {
            screenManager.showLogin(false);
        }
    }

    @Override
    public void render() {
        super.render();  // Important: calls current screen's render
    }

    @Override
    public void dispose() {
        if (networkClient != null) {
            networkClient.disconnect();
        }
        super.dispose();
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }

    public void startGame(Player player, boolean adminMode) {
        screenManager.showGame(player, adminMode);
    }

    public void openMapEditor() {
        screenManager.showMapEditor();
    }

    public boolean isProduction() {
        return isProduction;
    }
}