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
        font.getData().setScale(1.0f);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        createSkin();
        initUI();
    }

    protected void createSkin() {
        skin = new Skin();

        // Registrar fonte
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

        // Blue button texture (for primary)
        Pixmap bluePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bluePixmap.setColor(0.2f, 0.4f, 0.7f, 1);
        bluePixmap.fill();
        Texture blueTexture = new Texture(bluePixmap);
        bluePixmap.dispose();
        Drawable blueDrawable = new TextureRegionDrawable(blueTexture);

        // Window background
        Pixmap windowPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        windowPixmap.setColor(0.12f, 0.12f, 0.16f, 0.95f);
        windowPixmap.fill();
        Texture windowTexture = new Texture(windowPixmap);
        windowPixmap.dispose();
        Drawable windowDrawable = new TextureRegionDrawable(windowTexture);

        skin.add("window-bg", windowDrawable);

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

        Label.LabelStyle statusStyle = new Label.LabelStyle();
        statusStyle.font = font;
        statusStyle.fontColor = com.badlogic.gdx.graphics.Color.LIGHT_GRAY;
        skin.add("status", statusStyle);

        // Button styles
        TextButton.TextButtonStyle defaultButtonStyle = new TextButton.TextButtonStyle();
        defaultButtonStyle.font = font;
        defaultButtonStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        defaultButtonStyle.up = darkDrawable;
        defaultButtonStyle.down = darkDrawable;
        defaultButtonStyle.over = blueDrawable;
        skin.add("default", defaultButtonStyle);

        TextButton.TextButtonStyle adminButtonStyle = new TextButton.TextButtonStyle();
        adminButtonStyle.font = font;
        adminButtonStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        adminButtonStyle.up = greenDrawable;
        adminButtonStyle.down = greenDrawable;
        adminButtonStyle.over = blueDrawable;
        skin.add("admin", adminButtonStyle);

        TextButton.TextButtonStyle editorButtonStyle = new TextButton.TextButtonStyle();
        editorButtonStyle.font = font;
        editorButtonStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        editorButtonStyle.up = goldDrawable;
        editorButtonStyle.down = goldDrawable;
        editorButtonStyle.over = blueDrawable;
        skin.add("editor", editorButtonStyle);

        TextButton.TextButtonStyle primaryButtonStyle = new TextButton.TextButtonStyle();
        primaryButtonStyle.font = font;
        primaryButtonStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        primaryButtonStyle.up = blueDrawable;
        primaryButtonStyle.down = blueDrawable;
        primaryButtonStyle.over = darkDrawable;
        skin.add("primary", primaryButtonStyle);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        textFieldStyle.background = darkDrawable;
        textFieldStyle.cursor = blueDrawable;
        textFieldStyle.selection = blueDrawable;
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

        // ScrollPane style
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = windowDrawable;
        scrollStyle.vScroll = darkDrawable;
        scrollStyle.vScrollKnob = blueDrawable;
        scrollStyle.hScroll = darkDrawable;
        scrollStyle.hScrollKnob = blueDrawable;
        skin.add("default", scrollStyle);

        // SelectBox style
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = font;
        selectBoxStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        selectBoxStyle.background = darkDrawable;
        selectBoxStyle.scrollStyle = scrollStyle;

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = com.badlogic.gdx.graphics.Color.WHITE;
        listStyle.fontColorUnselected = com.badlogic.gdx.graphics.Color.LIGHT_GRAY;
        listStyle.selection = blueDrawable;
        listStyle.background = windowDrawable;
        selectBoxStyle.listStyle = listStyle;
        skin.add("default", selectBoxStyle);
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