package com.sandbox.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.common.sandbox.network.packets.FriendListResponse;
import com.common.sandbox.network.packets.PrivateMessagePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PrivateChatWindow {
    private static final Logger logger = LoggerFactory.getLogger(PrivateChatWindow.class);

    private Window window;
    private Skin skin;
    private Stage stage;
    private boolean visible = false;
    private FriendListResponse.FriendInfo currentFriend;

    private Label chatContentLabel;
    private ScrollPane chatScrollPane;
    private TextField messageField;
    private TextButton sendButton;
    private Label loadingLabel;

    private List<PrivateMessagePacket> messageHistory = new ArrayList<>();
    private BiConsumer<String, String> onSendMessage;
    private Consumer<String> onLoadHistory; // friendId

    private boolean isLoading = false;

    public PrivateChatWindow(Skin skin, Stage stage) {
        this.skin = skin;
        this.stage = stage;
        createWindow();
    }

    private void createWindow() {
        window = new Window("Chat Privado", skin, "default");
        window.setModal(false);
        window.setMovable(true);
        window.setSize(500, 520);
        window.setVisible(false);

        Table content = new Table();
        content.pad(10);

        // Chat header
        Label headerLabel = new Label("Chat Privado", skin, "title");
        headerLabel.setColor(Color.GOLD);
        content.add(headerLabel).center().padBottom(10);
        content.row();

        Label separator = new Label("----------------------------------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        content.add(separator).padBottom(10);
        content.row();

        // Loading indicator
        loadingLabel = new Label("Carregando historico...", skin, "status");
        loadingLabel.setColor(Color.YELLOW);
        loadingLabel.setVisible(false);
        content.add(loadingLabel).center().padBottom(5);
        content.row();

        // Chat content area
        chatContentLabel = new Label("", skin, "default");
        chatContentLabel.setWrap(true);
        chatContentLabel.setAlignment(Align.topLeft);
        chatContentLabel.setWidth(460);

        chatScrollPane = new ScrollPane(chatContentLabel, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollingDisabled(true, false);
        chatScrollPane.setSize(480, 300);

        content.add(chatScrollPane).width(480).height(300).padBottom(10);
        content.row();

        // Message input area
        Table inputTable = new Table();

        messageField = new TextField("", skin);
        messageField.setMessageText("Digite sua mensagem...");
        messageField.setSize(380, 35);

        sendButton = new TextButton("Enviar", skin, "primary");
        sendButton.setSize(80, 35);
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendCurrentMessage();
            }
        });

        inputTable.add(messageField).width(380).height(35).padRight(10);
        inputTable.add(sendButton).width(80).height(35);

        content.add(inputTable).width(480);
        content.row();

        // Hint
        Label hintLabel = new Label("Pressione ENTER para enviar mensagem", skin, "status");
        hintLabel.setFontScale(0.7f);
        hintLabel.setColor(Color.LIGHT_GRAY);
        content.add(hintLabel).center().padTop(5);
        content.row();

        // Close button
        TextButton closeButton = new TextButton("Fechar", skin, "default");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        content.add(closeButton).width(100).height(35).padTop(10);

        window.add(content).fill().expand();
        window.pack();
    }

    public void loadHistory(List<PrivateMessagePacket> messages) {
        logger.info("loadHistory called - messages count: {}", messages != null ? messages.size() : 0);

        isLoading = false;
        loadingLabel.setVisible(false);
        loadingLabel.setText("");

        // Limpar conteúdo atual
        chatContentLabel.setText("");

        if (messages == null || messages.isEmpty()) {
            logger.info("No messages in history");
            chatContentLabel.setText("Nenhuma mensagem ainda. Envie uma mensagem para começar!\n");
            return;
        }

        // Adicionar mensagens do histórico
        int addedCount = 0;
        for (PrivateMessagePacket msg : messages) {
            String senderName;
            if (currentFriend != null && msg.fromPlayerId.equals(currentFriend.playerId)) {
                senderName = currentFriend.username;
            } else {
                senderName = "Voce";
            }
            logger.debug("Adding message from {}: {}", senderName, msg.message);
            addMessage(senderName, msg.message, msg.timestamp);
            addedCount++;
        }

        logger.info("Added {} messages to chat", addedCount);

        // Scroll para o final
        chatScrollPane.setScrollPercentY(1);
    }

    public void sendCurrentMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && currentFriend != null && onSendMessage != null) {
            logger.info("Sending message to {}", currentFriend.username);
            onSendMessage.accept(currentFriend.playerId, message);
            messageField.setText("");
            addMessage("Voce", message, System.currentTimeMillis());
        }
    }

    public void addMessage(String senderName, String message, long timestamp) {
        String timeStr = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(timestamp));
        String formattedMsg = String.format("[%s] %s: %s", timeStr, senderName, message);

        logger.debug("Adding message to UI: {}", formattedMsg);

        String currentText = chatContentLabel.getText().toString();
        String newText = currentText + formattedMsg + "\n";

        String[] lines = newText.split("\n");
        if (lines.length > 200) {
            int firstNewline = newText.indexOf("\n");
            if (firstNewline > 0) {
                newText = newText.substring(firstNewline + 1);
            }
        }

        chatContentLabel.setText(newText);
        chatContentLabel.invalidateHierarchy();
        chatScrollPane.setScrollPercentY(1);
    }

    public void updateTitle(String title) {
        // Recriar o conteúdo com novo título na header
        String currentContent = chatContentLabel.getText().toString();
        float scrollY = chatScrollPane.getScrollY();

        window.clear();

        Table content = new Table();
        content.pad(10);

        Label headerLabel = new Label(title, skin, "title");
        headerLabel.setColor(Color.GOLD);
        content.add(headerLabel).center().padBottom(10);
        content.row();

        Label separator = new Label("----------------------------------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        content.add(separator).padBottom(10);
        content.row();

        content.add(loadingLabel).center().padBottom(5);
        content.row();

        chatContentLabel = new Label(currentContent, skin, "default");
        chatContentLabel.setWrap(true);
        chatContentLabel.setAlignment(Align.topLeft);
        chatContentLabel.setWidth(460);

        chatScrollPane = new ScrollPane(chatContentLabel, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollingDisabled(true, false);
        chatScrollPane.setSize(480, 300);
        chatScrollPane.setScrollY(scrollY);

        content.add(chatScrollPane).width(480).height(300).padBottom(10);
        content.row();

        Table inputTable = new Table();
        inputTable.add(messageField).width(380).height(35).padRight(10);
        inputTable.add(sendButton).width(80).height(35);

        content.add(inputTable).width(480);
        content.row();

        Label hintLabel = new Label("Pressione ENTER para enviar mensagem", skin, "status");
        hintLabel.setFontScale(0.7f);
        hintLabel.setColor(Color.LIGHT_GRAY);
        content.add(hintLabel).center().padTop(5);
        content.row();

        TextButton closeButton = new TextButton("Fechar", skin, "default");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        content.add(closeButton).width(100).height(35).padTop(10);

        window.add(content).fill().expand();
        window.pack();
    }

    public void openWithFriend(FriendListResponse.FriendInfo friend) {
        logger.info("Opening chat with friend: {}", friend.username);
        this.currentFriend = friend;
        updateTitle("Chat com " + friend.username);
        chatContentLabel.setText("");

        // Mostrar loading
        isLoading = true;
        loadingLabel.setVisible(true);
        loadingLabel.setText("Carregando historico...");

        // Carregar histórico
        if (onLoadHistory != null) {
            logger.info("Requesting history for friend: {}", friend.playerId);
            onLoadHistory.accept(friend.playerId);
        } else {
            logger.warn("onLoadHistory callback is null!");
            loadingLabel.setVisible(false);
            chatContentLabel.setText("Nao foi possivel carregar o historico.\n");
        }

        show();
        stage.setKeyboardFocus(messageField);
        messageField.selectAll();
    }

    public void show() {
        visible = true;
        window.setVisible(true);
        if (stage != null && window.getParent() == null) {
            stage.addActor(window);
        }
        window.toFront();
        stage.setKeyboardFocus(messageField);
        messageField.selectAll();
    }

    public void hide() {
        visible = false;
        window.setVisible(false);
        if (stage != null) {
            stage.setKeyboardFocus(null);
        }
    }

    public boolean isVisible() { return visible; }

    public void centerPosition(float screenWidth, float screenHeight) {
        window.setPosition(
                (screenWidth - window.getWidth()) / 2,
                (screenHeight - window.getHeight()) / 2
        );
    }

    public void setOnSendMessage(BiConsumer<String, String> callback) {
        this.onSendMessage = callback;
    }

    public void setOnLoadHistory(Consumer<String> callback) {
        this.onLoadHistory = callback;
    }

    public FriendListResponse.FriendInfo getCurrentFriend() {
        return currentFriend;
    }

    public void dispose() {
        window.clear();
    }
}