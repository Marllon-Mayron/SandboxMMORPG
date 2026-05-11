package com.sandbox.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.common.sandbox.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class PlayerUI {
    private static final Logger logger = LoggerFactory.getLogger(PlayerUI.class);

    private Stage stage;
    private Skin skin;
    private ShapeRenderer shapeRenderer;

    private Player currentPlayer;
    private float currentHealth = 100f;

    // Chat components
    private boolean chatVisible = true;
    private Label chatContentLabel;
    private Table chatContainer;
    private TextField chatInputField;
    private ScrollPane chatScrollPane;

    // UI components
    private Label playerNameLabel;
    private Label healthLabel;
    private Label healthPercentLabel;
    private Label speedLabel;
    private Label speedValueLabel;

    // Callback for sending messages
    private Consumer<String> sendMessageCallback;

    // Health bar dimensions
    private static final int HEALTH_BAR_WIDTH = 200;
    private static final int HEALTH_BAR_HEIGHT = 18;

    public PlayerUI(Skin skin) {
        this.skin = skin;
        this.stage = new Stage(new ScreenViewport());
        this.shapeRenderer = new ShapeRenderer();

        createUI();
    }

    private void createUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top().left();
        rootTable.pad(10);

        // ========== PLAYER INFO PANEL (Top-Left) ==========
        Table infoPanel = new Table(skin);
        if (skin.has("window-bg", Drawable.class)) {
            infoPanel.setBackground(skin.getDrawable("window-bg"));
        }
        infoPanel.pad(10);

        // Player name
        playerNameLabel = new Label("Player: ", skin, "default");
        playerNameLabel.setColor(Color.GOLD);
        playerNameLabel.setFontScale(1.1f);
        infoPanel.add(playerNameLabel).left().padBottom(5).colspan(2);
        infoPanel.row();

        // Health bar label
        healthLabel = new Label("Health:", skin, "default");
        infoPanel.add(healthLabel).left().padRight(10);

        // Health value (text)
        healthPercentLabel = new Label("100%", skin, "default");
        healthPercentLabel.setColor(Color.GREEN);
        infoPanel.add(healthPercentLabel).right();
        infoPanel.row();

        // Speed label
        speedLabel = new Label("Speed:", skin, "default");
        infoPanel.add(speedLabel).left().padRight(10);

        speedValueLabel = new Label("100%", skin, "default");
        speedValueLabel.setColor(Color.CYAN);
        infoPanel.add(speedValueLabel).right();

        rootTable.add(infoPanel).width(220);

        stage.addActor(rootTable);

        // ========== CHAT CONTAINER (Bottom-Left) ==========
        createChatContainer();
    }

    private void createChatContainer() {
        chatContainer = new Table(skin);
        if (skin.has("window-bg", Drawable.class)) {
            chatContainer.setBackground(skin.getDrawable("window-bg"));
        }
        chatContainer.pad(8);
        chatContainer.setSize(500, 280);

        // Posicionar no canto inferior esquerdo
        chatContainer.setPosition(10, 10);

        // Chat content (scrollable area)
        chatContentLabel = new Label("", skin, "default");
        chatContentLabel.setWrap(true);
        chatContentLabel.setAlignment(Align.topLeft);
        chatContentLabel.setWidth(480);

        // Chat scroll pane
        chatScrollPane = new ScrollPane(chatContentLabel, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollingDisabled(true, false);
        chatScrollPane.setSize(490, 200);

        chatContainer.add(chatScrollPane).width(490).height(200).padBottom(5);
        chatContainer.row();

        // Chat input field
        chatInputField = new TextField("", skin);
        chatInputField.setMessageText("Press ENTER to chat...");
        chatInputField.setSize(420, 35);

        TextButton sendButton = new TextButton("Send", skin, "primary");
        sendButton.setSize(70, 35);
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendCurrentMessage();
            }
        });

        Table inputTable = new Table();
        inputTable.add(chatInputField).width(420).padRight(5);
        inputTable.add(sendButton).width(70);
        chatContainer.add(inputTable).width(500);
        chatContainer.row();

        // Chat hint
        Label chatHint = new Label("Press ENTER to chat | H to hide/show chat", skin, "status");
        chatHint.setFontScale(0.7f);
        chatHint.setColor(Color.LIGHT_GRAY);
        chatContainer.add(chatHint).left().padTop(5);

        stage.addActor(chatContainer);
    }

    public void sendCurrentMessage() {
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
            if (sendMessageCallback != null) {
                sendMessageCallback.accept(message);
            }
            chatInputField.setText("");
            unfocusChat();
        }
    }

    public void focusChat() {
        stage.setKeyboardFocus(chatInputField);
        chatInputField.selectAll();
    }

    public void unfocusChat() {
        stage.setKeyboardFocus(null);
        chatInputField.setText("");
    }

    public boolean isChatFocused() {
        return stage.getKeyboardFocus() == chatInputField;
    }

    public void setChatInputProcessor(Consumer<String> callback) {
        this.sendMessageCallback = callback;
    }

    public void update(Player player, float terrainSpeed) {
        this.currentPlayer = player;

        if (player != null) {
            playerNameLabel.setText("Player: " + player.getUsername());
        }

        // Update speed display
        int speedPercent = Math.round(terrainSpeed * 100);
        speedValueLabel.setText(speedPercent + "%");

        if (terrainSpeed < 0.7f) {
            speedValueLabel.setColor(Color.RED);
        } else if (terrainSpeed < 0.95f) {
            speedValueLabel.setColor(Color.ORANGE);
        } else if (terrainSpeed > 1.05f) {
            speedValueLabel.setColor(Color.GREEN);
        } else {
            speedValueLabel.setColor(Color.CYAN);
        }

        // Update health percent display
        healthPercentLabel.setText(Math.round(currentHealth) + "%");

        // Health color effect
        if (currentHealth < 30f) {
            healthPercentLabel.setColor(Color.RED);
            healthLabel.setColor(Color.RED);
        } else if (currentHealth < 70f) {
            healthPercentLabel.setColor(Color.ORANGE);
            healthLabel.setColor(Color.ORANGE);
        } else {
            healthPercentLabel.setColor(Color.GREEN);
            healthLabel.setColor(Color.WHITE);
        }
    }

    public void setHealth(float health) {
        this.currentHealth = Math.max(0, Math.min(100, health));
    }

    public void damage(float amount) {
        this.currentHealth = Math.max(0, this.currentHealth - amount);
    }

    public void heal(float amount) {
        this.currentHealth = Math.min(100, this.currentHealth + amount);
    }

    public float getHealth() {
        return currentHealth;
    }

    public void addChatMessage(String message) {
        String currentText = chatContentLabel.getText().toString();
        String newText = currentText + message + "\n";

        // Limit to 100 lines
        String[] lines = newText.split("\n");
        if (lines.length > 100) {
            int firstNewline = newText.indexOf("\n");
            if (firstNewline > 0) {
                newText = newText.substring(firstNewline + 1);
            }
        }

        chatContentLabel.setText(newText);
        chatContentLabel.invalidateHierarchy();

        // Auto-scroll to bottom
        if (chatScrollPane != null) {
            chatScrollPane.setScrollPercentY(1);
        }
    }

    public void updateChatHistory(String chatHistory) {
        chatContentLabel.setText(chatHistory);
        chatContentLabel.invalidateHierarchy();
        if (chatScrollPane != null) {
            chatScrollPane.setScrollPercentY(1);
        }
    }

    public void clearChat() {
        chatContentLabel.setText("");
    }

    public void toggleChat() {
        chatVisible = !chatVisible;
        chatContainer.setVisible(chatVisible);

        // Só remove o foco se estiver ESCONDENDO o chat E o chat estava focado
        if (!chatVisible && isChatFocused()) {
            unfocusChat();
        }

        logger.info("Chat visibility: {}", chatVisible);
    }

    public void setChatVisible(boolean visible) {
        this.chatVisible = visible;
        chatContainer.setVisible(visible);
    }

    public boolean isChatVisible() {
        return chatVisible;
    }

    public void renderHealthBar(float x, float y) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background (dark red)
        shapeRenderer.setColor(0.3f, 0.1f, 0.1f, 0.8f);
        shapeRenderer.rect(x, y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);

        // Health fill
        float healthPercent = currentHealth / 100f;
        float fillWidth = HEALTH_BAR_WIDTH * healthPercent;

        if (healthPercent > 0.6f) {
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 0.9f);
        } else if (healthPercent > 0.3f) {
            shapeRenderer.setColor(0.9f, 0.7f, 0.2f, 0.9f);
        } else {
            shapeRenderer.setColor(0.9f, 0.2f, 0.2f, 0.9f);
        }
        shapeRenderer.rect(x + 2, y + 2, fillWidth - 4, HEALTH_BAR_HEIGHT - 4);

        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.8f, 0.8f, 0.8f, 1f);
        shapeRenderer.rect(x, y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        shapeRenderer.end();
    }

    public void render(SpriteBatch batch) {
        // Draw health bar below player info panel
        renderHealthBar(15, Gdx.graphics.getHeight() - 105);

        // Draw stage UI
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // Keep chat at bottom-left
        if (chatContainer != null) {
            chatContainer.setPosition(10, 10);
        }
    }
    public boolean isPointOverChat(int screenX, int screenY) {
        if (chatContainer == null || !chatContainer.isVisible()) return false;

        // Converter coordenadas de tela para coordenadas do stage
        com.badlogic.gdx.math.Vector2 local = new com.badlogic.gdx.math.Vector2(screenX, screenY);
        chatContainer.stageToLocalCoordinates(local);

        return local.x >= 0 && local.x <= chatContainer.getWidth() &&
                local.y >= 0 && local.y <= chatContainer.getHeight();
    }
    public Stage getStage() {
        return stage;
    }

    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (stage != null) stage.dispose();
    }
}