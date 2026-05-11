package com.sandbox.client.screens.auth;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.screens.AbstractScreen;

public class AdminModeSelectorScreen extends AbstractScreen {

    public AdminModeSelectorScreen(SandboxClient game) {
        super(game);
    }

    @Override
    protected void initUI() {
        Table table = new Table();
        centerTable(table);

        Label titleLabel = new Label("SANDBOX EXPERIMENT - SELECT MODE", skin, "title");
        titleLabel.setFontScale(2f);
        table.add(titleLabel).padBottom(50);
        table.row();

        Table buttonTable = new Table();

        TextButton normalButton = new TextButton("PLAY NORMAL MODE", skin, "default");
        normalButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getScreenManager().showLogin(false);
            }
        });
        buttonTable.add(normalButton).width(300).height(50).pad(10);
        buttonTable.row();

        TextButton adminButton = new TextButton("ADMIN MODE (Map Editor)", skin, "admin");
        adminButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getScreenManager().showLogin(true);
            }
        });
        buttonTable.add(adminButton).width(300).height(50).pad(10);

        table.add(buttonTable);

        Label infoLabel = new Label("Admin mode requires special credentials", skin, "default");
        infoLabel.setFontScale(0.8f);
        infoLabel.setColor(0.7f, 0.7f, 0.7f, 1);
        table.add(infoLabel).padTop(30);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render(delta);
    }
}