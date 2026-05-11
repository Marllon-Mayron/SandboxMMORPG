package com.sandbox.client.screens.game;

import com.badlogic.gdx.Screen;
import com.sandbox.client.GameWorldRenderer;
import com.sandbox.client.SandboxClient;
import com.common.sandbox.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class GameScreen implements Screen {
    private static final Logger logger = LoggerFactory.getLogger(GameScreen.class);

    private final SandboxClient game;
    private final Player player;
    private final boolean adminMode;
    private final Map<String, Player> nearbyPlayers;
    private GameWorldRenderer worldRenderer;

    public GameScreen(SandboxClient game, Player player, boolean adminMode, Map<String, Player> nearbyPlayers) {
        this.game = game;
        this.player = player;
        this.adminMode = adminMode;
        this.nearbyPlayers = nearbyPlayers;
    }

    @Override
    public void show() {
        logger.info("GameScreen show() - Creating GameWorldRenderer");
        worldRenderer = new GameWorldRenderer(game, adminMode, nearbyPlayers);
        worldRenderer.setCurrentPlayer(player);
        worldRenderer.show();
    }

    @Override
    public void render(float delta) {
        if (worldRenderer != null) {
            worldRenderer.render(delta);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (worldRenderer != null) {
            worldRenderer.resize(width, height);
        }
    }

    @Override
    public void pause() {
        if (worldRenderer != null) worldRenderer.pause();
    }

    @Override
    public void resume() {
        if (worldRenderer != null) worldRenderer.resume();
    }

    @Override
    public void hide() {
        if (worldRenderer != null) worldRenderer.hide();
    }

    @Override
    public void dispose() {
        if (worldRenderer != null) worldRenderer.dispose();
        logger.info("GameScreen disposed");
    }
}