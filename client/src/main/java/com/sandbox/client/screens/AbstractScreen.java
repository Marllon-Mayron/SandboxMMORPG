package com.sandbox.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.sandbox.client.SandboxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractScreen implements Screen {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractScreen.class);

    protected final SandboxClient game;
    protected Stage stage;
    protected Skin skin;
    protected SpriteBatch batch;
    protected BitmapFont font;
    protected OrthographicCamera camera;

    public AbstractScreen(SandboxClient game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        createSkin();
        initUI();
    }

    protected void createSkin() {
        skin = new Skin();
        skin.add("default-font", font);

        // Default button texture (dark gray)
        Pixmap darkPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        darkPixmap.setColor(0.2f, 0.2f, 0.2f, 1);
        darkPixmap.fill();
        Texture darkTexture = new Texture(darkPixmap);
        darkPixmap.dispose();
        Drawable darkDrawable = new TextureRegionDrawable(darkTexture);

        // Green button texture (for admin)
        Pixmap greenPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        greenPixmap.setColor(0.2f, 0.6f, 0.2f, 1);
        greenPixmap.fill();
        Texture greenTexture = new Texture(greenPixmap);
        greenPixmap.dispose();
        Drawable greenDrawable = new TextureRegionDrawable(greenTexture);

        // Gold button texture (for editor)
        Pixmap goldPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        goldPixmap.setColor(0.8f, 0.6f, 0.1f, 1);
        goldPixmap.fill();
        Texture goldTexture = new Texture(goldPixmap);
        goldPixmap.dispose();
        Drawable goldDrawable = new TextureRegionDrawable(goldTexture);

        // Label styles
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        skin.add("default", labelStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = font;
        titleStyle.fontColor = com.badlogic.gdx.graphics.Color.GOLD;
        skin.add("title", titleStyle);

        Label.LabelStyle errorStyle = new Label.LabelStyle();
        errorStyle.font = font;
        errorStyle.fontColor = com.badlogic.gdx.graphics.Color.RED;
        skin.add("error", errorStyle);

        Label.LabelStyle successStyle = new Label.LabelStyle();
        successStyle.font = font;
        successStyle.fontColor = com.badlogic.gdx.graphics.Color.GREEN;
        skin.add("success", successStyle);

        // Button styles
        TextButton.TextButtonStyle defaultButtonStyle = new TextButton.TextButtonStyle();
        defaultButtonStyle.font = font;
        defaultButtonStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        defaultButtonStyle.up = darkDrawable;
        defaultButtonStyle.down = darkDrawable;
        skin.add("default", defaultButtonStyle);

        TextButton.TextButtonStyle adminButtonStyle = new TextButton.TextButtonStyle();
        adminButtonStyle.font = font;
        adminButtonStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        adminButtonStyle.up = greenDrawable;
        adminButtonStyle.down = greenDrawable;
        skin.add("admin", adminButtonStyle);

        TextButton.TextButtonStyle editorButtonStyle = new TextButton.TextButtonStyle();
        editorButtonStyle.font = font;
        editorButtonStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        editorButtonStyle.up = goldDrawable;
        editorButtonStyle.down = goldDrawable;
        skin.add("editor", editorButtonStyle);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        textFieldStyle.background = darkDrawable;
        textFieldStyle.cursor = darkDrawable;
        textFieldStyle.selection = darkDrawable;
        skin.add("default", textFieldStyle);

        // CheckBox style
        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        Pixmap checkPixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        checkPixmap.setColor(0.2f, 0.8f, 0.2f, 1);
        checkPixmap.fill();
        Texture checkTexture = new Texture(checkPixmap);
        checkPixmap.dispose();
        checkBoxStyle.checkboxOn = new TextureRegionDrawable(checkTexture);
        checkBoxStyle.checkboxOff = darkDrawable;
        skin.add("default", checkBoxStyle);
    }

    protected abstract void initUI();

    protected void centerTable(Table table) {
        table.setFillParent(true);
        stage.addActor(table);
    }

    protected void updateViewport(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void render(float delta) {
        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        updateViewport(width, height);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}