package com.sandbox.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.common.sandbox.model.Player;
import com.sandbox.client.FontManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class PlayerUI {
    private static final Logger logger = LoggerFactory.getLogger(PlayerUI.class);

    private Stage stage;
    private Skin skin;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera uiCamera;  // Câmera separada para UI
    private FontManager fontManager;

    private Player currentPlayer;
    private float currentHealth = 100f;
    private float currentMana = 100f;
    private float currentStamina = 100f;
    private int currentGold = 0;
    private float terrainSpeed = 1.0f;

    private float screenWidth = 1280;
    private float screenHeight = 720;

    // Componentes do HUD
    private Label playerNameLabel;
    private Label goldLabel;
    private Label levelLabel;
    private Label healthLabel;
    private Label healthPercentLabel;
    private Label manaLabel;
    private Label manaPercentLabel;
    private Label staminaPercentLabel;
    private Label speedLabel;
    private Label speedValueLabel;

    // Componentes do Chat
    private boolean chatVisible = true;
    private Label chatContentLabel;
    private Table chatContainer;
    private TextField chatInputField;
    private ScrollPane chatScrollPane;
    private Consumer<String> sendMessageCallback;

    // Janela de Atributos
    private AttributesWindow attributesWindow;

    // Dimensões fixas do HUD e Chat
    private static final int HUD_WIDTH = 220;
    private static final int CHAT_WIDTH = 380;
    private static final int CHAT_HEIGHT = 280;

    // BARRA DE VIDA - VALORES ABSOLUTAMENTE FIXOS
    private static final int HEALTH_BAR_WIDTH = 220;
    private static final int HEALTH_BAR_HEIGHT = 20;
    private static final int HEALTH_BAR_X = 0;      // SEMPRE no canto esquerdo
    private static final int HEALTH_BAR_Y = 0;      // SEMPRE no topo

    // Posições em porcentagem para HUD e Chat
    private static final float HUD_X_PERCENT = 0.01f;
    private static final float HUD_Y_PERCENT = 0.72f;
    private static final float CHAT_X_PERCENT = 0.01f;
    private static final float CHAT_Y_PERCENT = 0.01f;

    public PlayerUI() {
        this.fontManager = FontManager.getInstance();
        this.shapeRenderer = new ShapeRenderer();
        this.stage = new Stage(new ScreenViewport());

        // Criar câmera separada para UI (coordenadas de tela)
        this.uiCamera = new OrthographicCamera();
        this.uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        createSkin();
        createUI();

        logger.info("PlayerUI initialized");
    }

    private void createSkin() {
        skin = new Skin();

        BitmapFont font = fontManager.getFont(FontManager.NORMAL);
        skin.add("default-font", font);

        Drawable darkBg = createColorDrawable(0.08f, 0.08f, 0.12f, 0.95f);
        Drawable buttonBg = createColorDrawable(0.2f, 0.2f, 0.28f, 1f);
        Drawable blueBg = createColorDrawable(0.2f, 0.45f, 0.8f, 1f);
        Drawable greenBg = createColorDrawable(0.2f, 0.65f, 0.25f, 1f);
        Drawable goldBg = createColorDrawable(0.85f, 0.65f, 0.15f, 1f);

        skin.add("window-bg", darkBg);
        skin.add("button-bg", buttonBg);
        skin.add("blue", blueBg);
        skin.add("green", greenBg);
        skin.add("gold", goldBg);

        // Label styles
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = font;
        titleStyle.fontColor = Color.GOLD;
        skin.add("title", titleStyle);

        Label.LabelStyle statusStyle = new Label.LabelStyle();
        statusStyle.font = font;
        statusStyle.fontColor = Color.LIGHT_GRAY;
        skin.add("status", statusStyle);

        Label.LabelStyle errorStyle = new Label.LabelStyle();
        errorStyle.font = font;
        errorStyle.fontColor = Color.RED;
        skin.add("error", errorStyle);

        Label.LabelStyle successStyle = new Label.LabelStyle();
        successStyle.font = font;
        successStyle.fontColor = Color.GREEN;
        skin.add("success", successStyle);

        // Button styles
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.up = buttonBg;
        textButtonStyle.down = darkBg;
        textButtonStyle.over = blueBg;
        skin.add("default", textButtonStyle);

        TextButton.TextButtonStyle primaryStyle = new TextButton.TextButtonStyle();
        primaryStyle.font = font;
        primaryStyle.fontColor = Color.WHITE;
        primaryStyle.up = blueBg;
        primaryStyle.down = darkBg;
        primaryStyle.over = buttonBg;
        skin.add("primary", primaryStyle);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = buttonBg;
        textFieldStyle.cursor = blueBg;
        textFieldStyle.selection = blueBg;
        skin.add("default", textFieldStyle);

        // ScrollPane style
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = darkBg;
        scrollStyle.vScroll = buttonBg;
        scrollStyle.vScrollKnob = blueBg;
        scrollStyle.hScroll = buttonBg;
        scrollStyle.hScrollKnob = blueBg;
        skin.add("default", scrollStyle);

        // Window style (para AttributesWindow)
        Window.WindowStyle windowStyle = new Window.WindowStyle(font, Color.GOLD, darkBg);
        skin.add("default", windowStyle);
    }

    private Drawable createColorDrawable(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    private void createUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top().left();
        rootTable.pad(10);

        // HUD PANEL
        Table infoPanel = new Table(skin);
        if (skin.has("window-bg", Drawable.class)) {
            infoPanel.setBackground(skin.getDrawable("window-bg"));
        }
        infoPanel.pad(10);

        playerNameLabel = new Label("Player: ", skin, "default");
        playerNameLabel.setColor(Color.GOLD);
        playerNameLabel.setFontScale(1.1f);
        infoPanel.add(playerNameLabel).left().padBottom(5).colspan(2);
        infoPanel.row();

        goldLabel = new Label("Gold: 0", skin, "default");
        goldLabel.setColor(Color.GOLD);
        infoPanel.add(goldLabel).left().padBottom(5).colspan(2);
        infoPanel.row();

        levelLabel = new Label("Level: 1", skin, "default");
        levelLabel.setColor(Color.CYAN);
        infoPanel.add(levelLabel).left().padBottom(5).colspan(2);
        infoPanel.row();

        Label separator = new Label("------------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        infoPanel.add(separator).left().padBottom(5).colspan(2);
        infoPanel.row();

        healthLabel = new Label("Health:", skin, "default");
        infoPanel.add(healthLabel).left().padRight(10);
        healthPercentLabel = new Label("100%", skin, "default");
        healthPercentLabel.setColor(Color.GREEN);
        infoPanel.add(healthPercentLabel).right();
        infoPanel.row();

        manaLabel = new Label("Mana:", skin, "default");
        infoPanel.add(manaLabel).left().padRight(10);
        manaPercentLabel = new Label("100%", skin, "default");
        manaPercentLabel.setColor(Color.CYAN);
        infoPanel.add(manaPercentLabel).right();
        infoPanel.row();

        Label staminaLabel = new Label("Stamina:", skin, "default");
        infoPanel.add(staminaLabel).left().padRight(10);
        staminaPercentLabel = new Label("100%", skin, "default");
        staminaPercentLabel.setColor(Color.LIME);
        infoPanel.add(staminaPercentLabel).right();
        infoPanel.row();

        infoPanel.add(separator).left().padTop(5).padBottom(5).colspan(2);
        infoPanel.row();

        speedLabel = new Label("Speed:", skin, "default");
        infoPanel.add(speedLabel).left().padRight(10);
        speedValueLabel = new Label("100%", skin, "default");
        speedValueLabel.setColor(Color.CYAN);
        infoPanel.add(speedValueLabel).right();

        rootTable.add(infoPanel).width(HUD_WIDTH);

        stage.addActor(rootTable);
    }

    private void createChatContainer() {
        chatContainer = new Table(skin);
        if (skin.has("window-bg", Drawable.class)) {
            chatContainer.setBackground(skin.getDrawable("window-bg"));
        }
        chatContainer.pad(8);
        chatContainer.setSize(CHAT_WIDTH, CHAT_HEIGHT);

        chatContentLabel = new Label("", skin, "default");
        chatContentLabel.setWrap(true);
        chatContentLabel.setAlignment(Align.topLeft);
        chatContentLabel.setWidth(CHAT_WIDTH - 20);

        chatScrollPane = new ScrollPane(chatContentLabel, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollingDisabled(true, false);
        chatScrollPane.setSize(CHAT_WIDTH - 10, CHAT_HEIGHT - 80);

        chatContainer.add(chatScrollPane).width(CHAT_WIDTH - 10).height(CHAT_HEIGHT - 80).padBottom(5);
        chatContainer.row();

        chatInputField = new TextField("", skin);
        chatInputField.setMessageText("Press ENTER to chat...");
        chatInputField.setSize(CHAT_WIDTH - 80, 35);

        TextButton sendButton = new TextButton("Send", skin, "primary");
        sendButton.setSize(70, 35);
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendCurrentMessage();
            }
        });

        Table inputTable = new Table();
        inputTable.add(chatInputField).width(CHAT_WIDTH - 80).padRight(5);
        inputTable.add(sendButton).width(70);
        chatContainer.add(inputTable).width(CHAT_WIDTH);
        chatContainer.row();

        Label chatHint = new Label("ENTER to chat | H to hide/show chat | C for attributes", skin, "status");
        chatHint.setFontScale(0.7f);
        chatHint.setColor(Color.LIGHT_GRAY);
        chatContainer.add(chatHint).left().padTop(5);

        stage.addActor(chatContainer);

        // Criar a janela de atributos
        attributesWindow = new AttributesWindow(skin, stage);
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public void sendCurrentMessage() {
        if (chatInputField == null) return;
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
        if (chatInputField != null) {
            stage.setKeyboardFocus(chatInputField);
            chatInputField.selectAll();
        }
    }

    public void unfocusChat() {
        stage.setKeyboardFocus(null);
        if (chatInputField != null) {
            chatInputField.setText("");
        }
    }

    public boolean isChatFocused() {
        return chatInputField != null && stage.getKeyboardFocus() == chatInputField;
    }

    public void setChatInputProcessor(Consumer<String> callback) {
        this.sendMessageCallback = callback;
        createChatContainer();
    }

    public void update(Player player, float terrainSpeed) {
        this.currentPlayer = player;
        this.terrainSpeed = terrainSpeed;

        if (player != null) {
            playerNameLabel.setText("Player: " + player.getUsername());
            goldLabel.setText("Gold: " + player.getGold());
            levelLabel.setText("Level: " + player.getLevel());
            currentGold = player.getGold();

            // Atualizar janela de atributos se visível
            if (attributesWindow != null && attributesWindow.isVisible()) {
                attributesWindow.update(player);
            }
        }

        healthPercentLabel.setText(Math.round(currentHealth) + "%");
        manaPercentLabel.setText(Math.round(currentMana) + "%");
        staminaPercentLabel.setText(Math.round(currentStamina) + "%");

        // Health color
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

        // Mana color
        if (currentMana < 30f) {
            manaPercentLabel.setColor(Color.RED);
            manaLabel.setColor(Color.RED);
        } else if (currentMana < 70f) {
            manaPercentLabel.setColor(Color.ORANGE);
            manaLabel.setColor(Color.ORANGE);
        } else {
            manaPercentLabel.setColor(Color.CYAN);
            manaLabel.setColor(Color.WHITE);
        }

        // Stamina color
        if (currentStamina < 30f) {
            staminaPercentLabel.setColor(Color.RED);
        } else if (currentStamina < 70f) {
            staminaPercentLabel.setColor(Color.ORANGE);
        } else {
            staminaPercentLabel.setColor(Color.LIME);
        }

        // Speed
        int speedPercent = Math.round(terrainSpeed * 100);
        speedValueLabel.setText(speedPercent + "%");

        if (terrainSpeed < 0.7f) {
            speedValueLabel.setColor(Color.RED);
            speedLabel.setColor(Color.RED);
        } else if (terrainSpeed < 0.95f) {
            speedValueLabel.setColor(Color.ORANGE);
            speedLabel.setColor(Color.ORANGE);
        } else if (terrainSpeed > 1.05f) {
            speedValueLabel.setColor(Color.GREEN);
            speedLabel.setColor(Color.GREEN);
        } else {
            speedValueLabel.setColor(Color.CYAN);
            speedLabel.setColor(Color.WHITE);
        }
    }

    public void setHealth(float percent) {
        this.currentHealth = Math.max(0, Math.min(100, percent));
    }

    public void setMana(float percent) {
        this.currentMana = Math.max(0, Math.min(100, percent));
    }

    public void setStamina(float percent) {
        this.currentStamina = Math.max(0, Math.min(100, percent));
    }

    public void setGold(int gold) {
        this.currentGold = gold;
        if (goldLabel != null) {
            goldLabel.setText("Gold: " + gold);
        }
    }

    public void addChatMessage(String message) {
        if (chatContentLabel == null) return;
        String currentText = chatContentLabel.getText().toString();
        String newText = currentText + message + "\n";

        String[] lines = newText.split("\n");
        if (lines.length > 100) {
            int firstNewline = newText.indexOf("\n");
            if (firstNewline > 0) {
                newText = newText.substring(firstNewline + 1);
            }
        }

        chatContentLabel.setText(newText);
        chatContentLabel.invalidateHierarchy();
        if (chatScrollPane != null) {
            chatScrollPane.setScrollPercentY(1);
        }
    }

    public void updateChatHistory(String chatHistory) {
        if (chatContentLabel == null) return;
        chatContentLabel.setText(chatHistory);
        chatContentLabel.invalidateHierarchy();
        if (chatScrollPane != null) {
            chatScrollPane.setScrollPercentY(1);
        }
    }

    public void toggleChat() {
        chatVisible = !chatVisible;
        if (chatContainer != null) {
            chatContainer.setVisible(chatVisible);
        }
        if (!chatVisible && isChatFocused()) {
            unfocusChat();
        }
    }

    public boolean isChatVisible() {
        return chatVisible;
    }

    public void toggleAttributes() {
        if (attributesWindow != null) {
            attributesWindow.toggle();
            if (attributesWindow.isVisible() && currentPlayer != null) {
                attributesWindow.update(currentPlayer);
                attributesWindow.centerPosition(screenWidth, screenHeight);
            }
        }
    }

    public boolean isAttributesVisible() {
        return attributesWindow != null && attributesWindow.isVisible();
    }

    public void hideAttributes() {
        if (attributesWindow != null) {
            attributesWindow.hide();
        }
    }

    // ==================== BARRA DE VIDA FIXA ====================

    private void renderHealthBar() {
        // Atualizar câmera da UI com as dimensões atuais da tela
        float currentWidth = Gdx.graphics.getWidth();
        float currentHeight = Gdx.graphics.getHeight();

        uiCamera.setToOrtho(false, currentWidth, currentHeight);
        uiCamera.update();

        // Usar a câmera da UI para o shapeRenderer
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        // Posição Y: topo da tela = altura atual - altura da barra
        float actualY = currentHeight - HEALTH_BAR_HEIGHT;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Fundo da barra (vermelho escuro)
        shapeRenderer.setColor(0.3f, 0.1f, 0.1f, 0.9f);
        shapeRenderer.rect(HEALTH_BAR_X, actualY, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);

        // Preenchimento da barra
        float healthPercent = currentHealth / 100f;
        float fillWidth = HEALTH_BAR_WIDTH * healthPercent;

        if (healthPercent > 0.6f) {
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 0.9f);  // Verde
        } else if (healthPercent > 0.3f) {
            shapeRenderer.setColor(0.9f, 0.7f, 0.2f, 0.9f);  // Laranja
        } else {
            shapeRenderer.setColor(0.9f, 0.2f, 0.2f, 0.9f);  // Vermelho
        }
        shapeRenderer.rect(HEALTH_BAR_X + 2, actualY + 2, Math.max(0, fillWidth - 4), HEALTH_BAR_HEIGHT - 4);

        shapeRenderer.end();

        // Borda da barra
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.9f, 0.9f, 0.9f, 1f);
        shapeRenderer.rect(HEALTH_BAR_X, actualY, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        shapeRenderer.end();

        // Desenhar texto da porcentagem ao lado da barra (usando câmera UI também)
        fontManager.begin();
        fontManager.getBatch().setProjectionMatrix(uiCamera.combined);
        String healthText = Math.round(currentHealth) + "%";
        fontManager.draw(FontManager.NORMAL, healthText, HEALTH_BAR_X + HEALTH_BAR_WIDTH + 8, actualY + HEALTH_BAR_HEIGHT - 4, Color.WHITE);
        fontManager.end();
    }

    // ==================== RENDER E RESIZE ====================

    public void render(SpriteBatch batch) {
        renderHealthBar();
        if (stage != null) {
            stage.act(Gdx.graphics.getDeltaTime());
            stage.draw();
        }
    }

    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        logger.info("UI resize called: {}x{}", width, height);

        // Atualizar câmera da UI
        uiCamera.setToOrtho(false, width, height);
        uiCamera.update();

        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }

        // Calcular posições baseadas em porcentagem para HUD e Chat
        float hudX = width * HUD_X_PERCENT;
        float hudY = height * HUD_Y_PERCENT;
        float chatXPos = width * CHAT_X_PERCENT;
        float chatYPos = height * CHAT_Y_PERCENT;

        if (chatContainer != null) {
            chatContainer.setPosition(chatXPos, chatYPos);
        }

        if (attributesWindow != null && attributesWindow.isVisible()) {
            attributesWindow.centerPosition(screenWidth, screenHeight);
        }
    }

    public boolean isPointOverChat(int screenX, int screenY) {
        if (chatContainer == null || !chatContainer.isVisible()) return false;

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
        if (skin != null) skin.dispose();
        if (attributesWindow != null) attributesWindow.dispose();
        logger.info("PlayerUI disposed");
    }
}