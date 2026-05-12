package com.sandbox.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class UISkinManager {

    public static Skin createDefaultSkin(BitmapFont font) {
        Skin skin = new Skin();

        skin.add("default-font", font);

        Drawable darkBg = createColorDrawable(0.08f, 0.08f, 0.12f, 0.95f);
        Drawable buttonBg = createColorDrawable(0.2f, 0.2f, 0.28f, 1f);
        Drawable blueBg = createColorDrawable(0.2f, 0.45f, 0.8f, 1f);
        Drawable greenBg = createColorDrawable(0.2f, 0.65f, 0.25f, 1f);
        Drawable goldBg = createColorDrawable(0.85f, 0.65f, 0.15f, 1f);
        Drawable redBg = createColorDrawable(0.75f, 0.2f, 0.2f, 1f);

        skin.add("window-bg", darkBg);
        skin.add("button-bg", buttonBg);
        skin.add("blue", blueBg);
        skin.add("green", greenBg);
        skin.add("gold", goldBg);
        skin.add("red", redBg);

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

        TextButton.TextButtonStyle adminStyle = new TextButton.TextButtonStyle();
        adminStyle.font = font;
        adminStyle.fontColor = Color.WHITE;
        adminStyle.up = greenBg;
        adminStyle.down = darkBg;
        adminStyle.over = blueBg;
        skin.add("admin", adminStyle);

        TextButton.TextButtonStyle editorStyle = new TextButton.TextButtonStyle();
        editorStyle.font = font;
        editorStyle.fontColor = Color.WHITE;
        editorStyle.up = goldBg;
        editorStyle.down = darkBg;
        editorStyle.over = blueBg;
        skin.add("editor", editorStyle);

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

        // CheckBox style
        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.WHITE;
        checkBoxStyle.checkboxOn = greenBg;
        checkBoxStyle.checkboxOff = buttonBg;
        skin.add("default", checkBoxStyle);

        // SelectBox style
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = font;
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = buttonBg;
        selectBoxStyle.scrollStyle = scrollStyle;

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.LIGHT_GRAY;
        listStyle.selection = blueBg;
        listStyle.background = darkBg;
        selectBoxStyle.listStyle = listStyle;
        skin.add("default", selectBoxStyle);

        return skin;
    }

    private static Drawable createColorDrawable(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }
}