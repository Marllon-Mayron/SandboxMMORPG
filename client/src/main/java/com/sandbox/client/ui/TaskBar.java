package com.sandbox.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class TaskBar {
    private Table taskBar;
    private Skin skin;
    private Runnable onFriendsClick;
    private Runnable onAttributesClick;

    private static final int TASK_BAR_WIDTH = 220;
    private static final int TASK_BAR_HEIGHT = 50;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 40;

    public TaskBar(Skin skin, Stage stage) {
        this.skin = skin;
        createTaskBar(stage);
    }

    private void createTaskBar(Stage stage) {
        taskBar = new Table(skin);
        taskBar.setBackground(createTaskBarBackground());
        taskBar.setSize(TASK_BAR_WIDTH, TASK_BAR_HEIGHT);

        // Botao Amigos
        TextButton friendsButton = new TextButton("Amigos", skin, "primary");
        friendsButton.getLabel().setFontScale(1.0f);
        friendsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("Friends button clicked!"); // Debug
                if (onFriendsClick != null) {
                    onFriendsClick.run();
                }
            }
        });
        taskBar.add(friendsButton).width(BUTTON_WIDTH).height(BUTTON_HEIGHT).pad(5);

        // Botao Atributos
        TextButton attributesButton = new TextButton("Atributos", skin, "default");
        attributesButton.getLabel().setFontScale(1.0f);
        attributesButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("Attributes button clicked!"); // Debug
                if (onAttributesClick != null) {
                    onAttributesClick.run();
                }
            }
        });
        taskBar.add(attributesButton).width(BUTTON_WIDTH).height(BUTTON_HEIGHT).pad(5);

        stage.addActor(taskBar);

        // Garantir que a taskbar está no topo
        taskBar.toFront();
    }

    private Drawable createTaskBarBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.08f, 0.08f, 0.12f, 0.95f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    public void setPosition(float x, float y) {
        taskBar.setPosition(x, y);
    }

    public void setOnFriendsClick(Runnable runnable) {
        this.onFriendsClick = runnable;
    }

    public void setOnAttributesClick(Runnable runnable) {
        this.onAttributesClick = runnable;
    }

    public void setVisible(boolean visible) {
        taskBar.setVisible(visible);
    }

    public void toFront() {
        taskBar.toFront();
    }

    public void dispose() {
        taskBar.clear();
    }
}