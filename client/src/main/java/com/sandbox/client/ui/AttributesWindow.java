package com.sandbox.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.common.sandbox.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributesWindow {
    private static final Logger logger = LoggerFactory.getLogger(AttributesWindow.class);

    private Window window;
    private Skin skin;
    private boolean visible = false;
    private Player currentPlayer;
    private Stage stage;

    // Labels
    private Label levelValueLabel;
    private Label hpValueLabel;
    private Label manaValueLabel;
    private Label staminaValueLabel;
    private Label strengthValueLabel;
    private Label agilityValueLabel;
    private Label wisdomValueLabel;
    private Label attributePointsValueLabel;
    private Label skillPointsValueLabel;
    private Label experienceValueLabel;
    private Label nextLevelXpLabel;

    public AttributesWindow(Skin skin, Stage stage) {
        this.skin = skin;
        this.stage = stage;
        createWindow();
    }

    private void createWindow() {
        // Window background
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.05f, 0.05f, 0.08f, 0.98f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        Drawable windowBackground = new TextureRegionDrawable(texture);

        // Border
        Pixmap borderPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        borderPixmap.setColor(0.4f, 0.3f, 0.1f, 1f);
        borderPixmap.fill();
        Texture borderTexture = new Texture(borderPixmap);
        borderPixmap.dispose();
        Drawable borderDrawable = new TextureRegionDrawable(borderTexture);

        BitmapFont font = skin.getFont("default-font");

        Window.WindowStyle windowStyle = new Window.WindowStyle(font, Color.GOLD, windowBackground);
        window = new Window(" CHARACTER ATTRIBUTES ", windowStyle);
        window.setModal(true);
        window.setMovable(true);
        window.setSize(380, 500);
        window.setVisible(false);

        // Add border using a table with background
        Table mainTable = new Table();
        mainTable.setBackground(borderDrawable);

        Table content = new Table();
        content.pad(15);
        content.setBackground(windowBackground);

        // Title bar
        Label titleLabel = new Label("------ STATUS ------", skin, "title");
        titleLabel.setColor(Color.GOLD);
        content.add(titleLabel).colspan(2).center().padBottom(15);
        content.row();

        addSeparator(content);

        // Level & Experience
        addStatRow(content, "Level:", levelValueLabel = createValueLabel(), Color.CYAN);
        addStatRow(content, "Experience:", experienceValueLabel = createValueLabel(), Color.GOLD);
        addStatRow(content, "Next Level:", nextLevelXpLabel = createValueLabel(), Color.LIGHT_GRAY);

        addSeparator(content);

        // Combat Stats
        Label combatTitle = new Label("COMBAT STATS", skin, "title");
        combatTitle.setColor(Color.CORAL);
        content.add(combatTitle).colspan(2).center().padTop(5).padBottom(10);
        content.row();
        addThinSeparator(content);

        addStatRow(content, "Health:", hpValueLabel = createValueLabel(), Color.GREEN);
        addStatRow(content, "Mana:", manaValueLabel = createValueLabel(), Color.CYAN);
        addStatRow(content, "Stamina:", staminaValueLabel = createValueLabel(), Color.LIME);

        addSeparator(content);

        // Attributes
        Label attrTitle = new Label("ATTRIBUTES", skin, "title");
        attrTitle.setColor(Color.CORAL);
        content.add(attrTitle).colspan(2).center().padTop(5).padBottom(10);
        content.row();
        addThinSeparator(content);

        addStatRow(content, "Strength:", strengthValueLabel = createValueLabel(), Color.ORANGE);
        addStatRow(content, "Agility:", agilityValueLabel = createValueLabel(), Color.GREEN);
        addStatRow(content, "Wisdom:", wisdomValueLabel = createValueLabel(), Color.PURPLE);

        addSeparator(content);

        // Points
        Label pointsTitle = new Label("AVAILABLE POINTS", skin, "title");
        pointsTitle.setColor(Color.CORAL);
        content.add(pointsTitle).colspan(2).center().padTop(5).padBottom(10);
        content.row();
        addThinSeparator(content);

        addStatRow(content, "Attribute Points:", attributePointsValueLabel = createValueLabel(), Color.YELLOW);
        addStatRow(content, "Skill Points:", skillPointsValueLabel = createValueLabel(), Color.PINK);

        addSeparator(content);

        // Close button
        TextButton closeButton = new TextButton("CLOSE", skin, "primary");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        content.add(closeButton).colspan(2).center().width(180).height(40).padTop(15);

        mainTable.add(content).fill().expand();
        window.add(mainTable).fill().expand();
        window.pack();
    }

    private Label createValueLabel() {
        Label label = new Label("0", skin, "default");
        return label;
    }

    private void addStatRow(Table table, String labelText, Label valueLabel, Color valueColor) {
        Label statLabel = new Label(labelText, skin, "default");
        statLabel.setColor(Color.LIGHT_GRAY);
        table.add(statLabel).left().padRight(20).padBottom(6);
        valueLabel.setColor(valueColor);
        table.add(valueLabel).right().padBottom(6);
        table.row();
    }

    private void addSeparator(Table table) {
        Label separator = new Label("---------------------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        table.add(separator).colspan(2).center().padTop(8).padBottom(8);
        table.row();
    }

    private void addThinSeparator(Table table) {
        Label separator = new Label("-------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        table.add(separator).colspan(2).center().padTop(3).padBottom(3);
        table.row();
    }

    public void update(Player player) {
        this.currentPlayer = player;
        if (player == null) return;

        levelValueLabel.setText(String.valueOf(player.getLevel()));
        experienceValueLabel.setText(player.getExperience() + " / " + player.getXpForNextLevel());
        nextLevelXpLabel.setText(player.getXpForNextLevel() + " XP");

        hpValueLabel.setText(player.getCurrentHp() + " / " + player.getMaxHp());
        manaValueLabel.setText(player.getCurrentMana() + " / " + player.getMaxMana());
        staminaValueLabel.setText(player.getCurrentStamina() + " / " + player.getMaxStamina());

        strengthValueLabel.setText(String.valueOf(player.getStrength()));
        agilityValueLabel.setText(String.valueOf(player.getAgility()));
        wisdomValueLabel.setText(String.valueOf(player.getWisdom()));

        attributePointsValueLabel.setText(String.valueOf(player.getAttributePoints()));
        skillPointsValueLabel.setText(String.valueOf(player.getSkillPoints()));

        // Color points if available
        if (player.getAttributePoints() > 0) {
            attributePointsValueLabel.setColor(Color.GREEN);
        } else {
            attributePointsValueLabel.setColor(Color.YELLOW);
        }

        if (player.getSkillPoints() > 0) {
            skillPointsValueLabel.setColor(Color.GREEN);
        } else {
            skillPointsValueLabel.setColor(Color.PINK);
        }
    }

    public void show() {
        visible = true;
        window.setVisible(true);
        if (currentPlayer != null) {
            update(currentPlayer);
        }
        if (stage != null && window.getParent() == null) {
            stage.addActor(window);
        }
    }

    public void hide() {
        visible = false;
        window.setVisible(false);
    }

    public void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void centerPosition(float screenWidth, float screenHeight) {
        window.setPosition(
                (screenWidth - window.getWidth()) / 2,
                (screenHeight - window.getHeight()) / 2
        );
    }

    public void dispose() {
        window.clear();
    }
}