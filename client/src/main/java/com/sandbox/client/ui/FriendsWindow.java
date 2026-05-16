package com.sandbox.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.common.sandbox.network.packets.social.FriendListResponse;

import java.util.List;
import java.util.function.Consumer;

public class FriendsWindow {
    private Window window;
    private Skin skin;
    private Stage stage;
    private boolean visible = false;

    private TextField searchField;
    private TextButton addButton;
    private Table friendsTable;
    private Table requestsTable;
    private ScrollPane friendsScroll;
    private ScrollPane requestsScroll;
    private Table tabButtons;
    private Table currentContent;
    private int currentTab = 0;

    private Consumer<String> onSendFriendRequest;
    private Consumer<String> onAcceptFriendRequest;
    private Consumer<String> onRejectFriendRequest;
    private Consumer<String> onRemoveFriend;
    private Consumer<FriendListResponse.FriendInfo> onOpenPrivateChat;

    private List<FriendListResponse.FriendInfo> currentFriends;
    private List<FriendListResponse.FriendRequestInfo> currentRequests;

    public FriendsWindow(Skin skin, Stage stage) {
        this.skin = skin;
        this.stage = stage;
        createWindow();
    }

    private void createWindow() {
        window = new Window("Amigos", skin, "default");
        window.setModal(false);
        window.setMovable(true);
        window.setSize(450, 520);
        window.setVisible(false);

        Table mainContent = new Table();
        mainContent.pad(10);

        // Add friend section
        Table addSection = new Table();
        addSection.setBackground(createSectionBackground());
        addSection.pad(8);

        searchField = new TextField("", skin);
        searchField.setMessageText("Digite o nome do amigo...");

        // Adicionar listener para capturar ENTER no campo de texto
        searchField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Mudança de texto - não fazer nada
            }
        });

        addButton = new TextButton("Adicionar", skin, "primary");
        addButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String username = searchField.getText().trim();
                if (!username.isEmpty() && onSendFriendRequest != null) {
                    onSendFriendRequest.accept(username);
                    searchField.setText("");
                }
                // Voltar foco para o stage principal
                Gdx.input.setInputProcessor(stage);
            }
        });

        addSection.add(searchField).width(220).height(35).padRight(10);
        addSection.add(addButton).width(100).height(35);

        mainContent.add(addSection).width(420).padBottom(10);
        mainContent.row();

        // Tab buttons
        tabButtons = new Table();

        TextButton friendsTabBtn = new TextButton("Amigos", skin, "default");
        friendsTabBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchTab(0);
            }
        });

        TextButton requestsTabBtn = new TextButton("Solicitacoes", skin, "default");
        requestsTabBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchTab(1);
            }
        });

        tabButtons.add(friendsTabBtn).width(200).height(35).padRight(5);
        tabButtons.add(requestsTabBtn).width(200).height(35).padLeft(5);

        mainContent.add(tabButtons).width(420).padBottom(10);
        mainContent.row();

        // Content area
        currentContent = new Table();
        currentContent.setBackground(createSectionBackground());
        currentContent.setSize(420, 320);

        // Friends tab content
        friendsTable = new Table();
        friendsTable.top().left();
        friendsScroll = new ScrollPane(friendsTable, skin);
        friendsScroll.setFadeScrollBars(false);
        friendsScroll.setScrollingDisabled(true, false);
        friendsScroll.setSize(400, 300);

        // Requests tab content
        requestsTable = new Table();
        requestsTable.top().left();
        requestsScroll = new ScrollPane(requestsTable, skin);
        requestsScroll.setFadeScrollBars(false);
        requestsScroll.setScrollingDisabled(true, false);
        requestsScroll.setSize(400, 300);

        currentContent.add(friendsScroll).width(400).height(300).pad(10);

        mainContent.add(currentContent).width(420).height(320).padBottom(10);
        mainContent.row();

        // Close button
        TextButton closeButton = new TextButton("Fechar", skin, "default");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        mainContent.add(closeButton).width(100).height(35).padTop(10);

        window.add(mainContent).fill().expand();
        window.pack();
    }

    private void switchTab(int tab) {
        currentTab = tab;
        currentContent.clear();

        if (tab == 0) {
            currentContent.add(friendsScroll).width(400).height(300).pad(10);
        } else {
            currentContent.add(requestsScroll).width(400).height(300).pad(10);
        }
    }

    private Drawable createSectionBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.12f, 0.12f, 0.16f, 0.95f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    public void updateFriends(List<FriendListResponse.FriendInfo> friends) {
        this.currentFriends = friends;
        friendsTable.clear();

        if (friends == null || friends.isEmpty()) {
            Label emptyLabel = new Label("Nenhum amigo adicionado ainda.", skin, "status");
            emptyLabel.setColor(Color.LIGHT_GRAY);
            friendsTable.add(emptyLabel).pad(20);
            return;
        }

        for (FriendListResponse.FriendInfo friend : friends) {
            Table friendRow = new Table();
            friendRow.setBackground(createSectionBackground());
            friendRow.pad(8);

            String statusText = friend.isOnline ? "[ONLINE]" : "[OFFLINE]";
            Color statusColor = friend.isOnline ? Color.GREEN : Color.DARK_GRAY;

            Label statusLabel = new Label(statusText, skin, "default");
            statusLabel.setColor(statusColor);

            Label nameLabel = new Label(friend.username + " (Lv." + friend.level + ")", skin, "default");
            nameLabel.setColor(friend.isOnline ? Color.WHITE : Color.LIGHT_GRAY);
            nameLabel.setAlignment(Align.left);

            TextButton chatButton = new TextButton("Msg", skin, "primary");
            chatButton.setSize(50, 25);
            chatButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (onOpenPrivateChat != null) onOpenPrivateChat.accept(friend);
                    hide();
                }
            });

            TextButton removeButton = new TextButton("Remover", skin, "default");
            removeButton.setSize(60, 25);
            removeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (onRemoveFriend != null) onRemoveFriend.accept(friend.username);
                }
            });

            friendRow.add(statusLabel).width(70);
            friendRow.add(nameLabel).width(160).padLeft(5);
            friendRow.add(chatButton).width(50).height(25).padLeft(5);
            friendRow.add(removeButton).width(60).height(25).padLeft(5);

            friendsTable.add(friendRow).width(390).padBottom(5);
            friendsTable.row();
        }
    }

    public void updateRequests(List<FriendListResponse.FriendRequestInfo> requests) {
        this.currentRequests = requests;
        requestsTable.clear();

        if (requests == null || requests.isEmpty()) {
            Label emptyLabel = new Label("Nenhuma solicitacao pendente.", skin, "status");
            emptyLabel.setColor(Color.LIGHT_GRAY);
            requestsTable.add(emptyLabel).pad(20);
            return;
        }

        for (FriendListResponse.FriendRequestInfo request : requests) {
            Table requestRow = new Table();
            requestRow.setBackground(createSectionBackground());
            requestRow.pad(8);

            Label nameLabel = new Label(request.fromUsername + " (Lv." + request.fromLevel + ")", skin, "default");
            nameLabel.setAlignment(Align.left);

            TextButton acceptButton = new TextButton("Aceitar", skin, "primary");
            acceptButton.setSize(70, 25);
            acceptButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (onAcceptFriendRequest != null) onAcceptFriendRequest.accept(request.requestId);
                }
            });

            TextButton rejectButton = new TextButton("Recusar", skin, "default");
            rejectButton.setSize(70, 25);
            rejectButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (onRejectFriendRequest != null) onRejectFriendRequest.accept(request.requestId);
                }
            });

            requestRow.add(nameLabel).width(180).padLeft(5);
            requestRow.add(acceptButton).width(70).height(25).padLeft(5);
            requestRow.add(rejectButton).width(70).height(25).padLeft(5);

            requestsTable.add(requestRow).width(390).padBottom(5);
            requestsTable.row();
        }
    }

    public void show() {
        visible = true;
        window.setVisible(true);
        if (stage != null && window.getParent() == null) {
            stage.addActor(window);
        }
        window.toFront();
        // Dar foco ao campo de texto automaticamente
        stage.setKeyboardFocus(searchField);
    }

    public void hide() {
        visible = false;
        window.setVisible(false);
        if (stage != null) {
            stage.setKeyboardFocus(null);
        }
    }

    public void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    public boolean isVisible() { return visible; }

    public void centerPosition(float screenWidth, float screenHeight) {
        window.setPosition(
                (screenWidth - window.getWidth()) / 2,
                (screenHeight - window.getHeight()) / 2
        );
    }

    public void setOnSendFriendRequest(Consumer<String> callback) {
        this.onSendFriendRequest = callback;
    }

    public void setOnAcceptFriendRequest(Consumer<String> callback) {
        this.onAcceptFriendRequest = callback;
    }

    public void setOnRejectFriendRequest(Consumer<String> callback) {
        this.onRejectFriendRequest = callback;
    }

    public void setOnRemoveFriend(Consumer<String> callback) {
        this.onRemoveFriend = callback;
    }

    public void setOnOpenPrivateChat(Consumer<FriendListResponse.FriendInfo> callback) {
        this.onOpenPrivateChat = callback;
    }

    public void dispose() {
        window.clear();
    }
}