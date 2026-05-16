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
import com.common.sandbox.model.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributesWindow {
    private static final Logger logger = LoggerFactory.getLogger(AttributesWindow.class);

    private Window window;
    private Skin skin;
    private boolean visible = false;
    private Player currentPlayer;
    private Stage stage;

    // Primary Stats Labels
    private Label levelValueLabel;
    private Label experienceValueLabel;
    private Label experienceProgressLabel;
    private Label nextLevelXpLabel;

    // Combat Stats Labels
    private Label hpValueLabel;
    private Label manaValueLabel;
    private Label staminaValueLabel;
    private Label hpRegenLabel;
    private Label manaRegenLabel;
    private Label staminaRegenLabel;

    // Movement Stats Labels
    private Label baseSpeedLabel;
    private Label sprintMultiplierLabel;
    private Label dashDistanceLabel;
    private Label dashCooldownLabel;
    private Label dashCostLabel;

    // Attributes Labels
    private Label strengthValueLabel;
    private Label agilityValueLabel;
    private Label wisdomValueLabel;

    // Points Labels
    private Label attributePointsValueLabel;
    private Label skillPointsValueLabel;

    // Progress bar for experience
    private ProgressBar expProgressBar;

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
        borderPixmap.setColor(0.35f, 0.25f, 0.10f, 1f);
        borderPixmap.fill();
        Texture borderTexture = new Texture(borderPixmap);
        borderPixmap.dispose();
        Drawable borderDrawable = new TextureRegionDrawable(borderTexture);

        // Progress bar style
        Pixmap barBgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        barBgPixmap.setColor(0.15f, 0.10f, 0.05f, 1f);
        barBgPixmap.fill();
        Texture barBgTexture = new Texture(barBgPixmap);
        barBgPixmap.dispose();
        Drawable barBg = new TextureRegionDrawable(barBgTexture);

        Pixmap barFillPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        barFillPixmap.setColor(0.85f, 0.65f, 0.15f, 1f);
        barFillPixmap.fill();
        Texture barFillTexture = new Texture(barFillPixmap);
        barFillPixmap.dispose();
        Drawable barFill = new TextureRegionDrawable(barFillTexture);

        ProgressBar.ProgressBarStyle progressStyle = new ProgressBar.ProgressBarStyle();
        progressStyle.background = barBg;
        progressStyle.knobBefore = barFill;

        BitmapFont font = skin.getFont("default-font");

        Window.WindowStyle windowStyle = new Window.WindowStyle(font, Color.GOLD, windowBackground);
        window = new Window(" C H A R A C T E R   I N F O R M A T I O N ", windowStyle);
        window.setModal(true);
        window.setMovable(true);
        window.setSize(460, 620);
        window.setVisible(false);

        Table mainTable = new Table();
        mainTable.setBackground(borderDrawable);

        Table content = new Table();
        content.pad(15);
        content.setBackground(windowBackground);

        // Header
        Label headerLabel = new Label("S T A T U S", skin, "title");
        headerLabel.setColor(Color.GOLD);
        headerLabel.setFontScale(1.2f);
        content.add(headerLabel).colspan(2).center().padBottom(15);
        content.row();

        addSeparator(content);

        // Section: PROGRESSION
        Label progressionTitle = new Label("PROGRESSION", skin, "title");
        progressionTitle.setColor(Color.CYAN);
        content.add(progressionTitle).colspan(2).left().padBottom(8);
        content.row();
        addThinSeparator(content);

        addStatRow(content, "Level:", levelValueLabel = createValueLabel(), Color.CYAN);

        // Experience with progress bar
        Table expTable = new Table();
        experienceValueLabel = createValueLabel();
        experienceProgressLabel = createValueLabel();

        expTable.add(new Label("Experience:", skin, "default")).left().padRight(20);
        expTable.add(experienceValueLabel).right();
        expTable.row();
        expTable.add(new Label("Progress:", skin, "default")).left().padRight(20);
        expTable.add(experienceProgressLabel).right();

        content.add(expTable).colspan(2).padBottom(6);
        content.row();

        addStatRow(content, "Next Level:", nextLevelXpLabel = createValueLabel(), Color.LIGHT_GRAY);

        // Experience progress bar
        expProgressBar = new ProgressBar(0, 1, 0.01f, false, progressStyle);
        expProgressBar.setSize(380, 12);
        content.add(expProgressBar).colspan(2).width(380).height(12).padTop(5).padBottom(10);
        content.row();

        addSeparator(content);

        // Section: COMBAT STATUS
        Label combatTitle = new Label("C O M B A T   S T A T U S", skin, "title");
        combatTitle.setColor(Color.CORAL);
        content.add(combatTitle).colspan(2).left().padBottom(8);
        content.row();
        addThinSeparator(content);

        addStatRow(content, "Health:", hpValueLabel = createValueLabel(), Color.GREEN);
        addStatRow(content, "Mana:", manaValueLabel = createValueLabel(), Color.CYAN);
        addStatRow(content, "Stamina:", staminaValueLabel = createValueLabel(), Color.LIME);

        // Regen info
        Table regenTable = new Table();
        hpRegenLabel = createValueLabel();
        manaRegenLabel = createValueLabel();
        staminaRegenLabel = createValueLabel();

        regenTable.add(new Label("HP Regen:", skin, "default")).left().padRight(15);
        regenTable.add(hpRegenLabel).right().padRight(25);
        regenTable.add(new Label("MP Regen:", skin, "default")).left().padRight(15);
        regenTable.add(manaRegenLabel).right().padRight(25);
        regenTable.add(new Label("SP Regen:", skin, "default")).left().padRight(15);
        regenTable.add(staminaRegenLabel).right();

        content.add(regenTable).colspan(2).padTop(5).padBottom(10);
        content.row();

        addSeparator(content);

        // Section: MOVEMENT
        Label movementTitle = new Label("M O V E M E N T", skin, "title");
        movementTitle.setColor(Color.YELLOW);
        content.add(movementTitle).colspan(2).left().padBottom(8);
        content.row();
        addThinSeparator(content);

        baseSpeedLabel = createValueLabel();
        sprintMultiplierLabel = createValueLabel();
        dashDistanceLabel = createValueLabel();
        dashCooldownLabel = createValueLabel();
        dashCostLabel = createValueLabel();

        Table movementTable = new Table();
        movementTable.add(new Label("Base Speed:", skin, "default")).left().padRight(15);
        movementTable.add(baseSpeedLabel).right().padRight(25);
        movementTable.add(new Label("Sprint Mult:", skin, "default")).left().padRight(15);
        movementTable.add(sprintMultiplierLabel).right();
        movementTable.row();
        movementTable.add(new Label("Dash Dist:", skin, "default")).left().padRight(15);
        movementTable.add(dashDistanceLabel).right().padRight(25);
        movementTable.add(new Label("Dash CD:", skin, "default")).left().padRight(15);
        movementTable.add(dashCooldownLabel).right();
        movementTable.row();
        movementTable.add(new Label("Dash Cost:", skin, "default")).left().padRight(15);
        movementTable.add(dashCostLabel).right();

        content.add(movementTable).colspan(2).padBottom(10);
        content.row();

        addSeparator(content);

        // Section: ATTRIBUTES
        Label attrTitle = new Label("A T T R I B U T E S", skin, "title");
        attrTitle.setColor(Color.ORANGE);
        content.add(attrTitle).colspan(2).left().padBottom(8);
        content.row();
        addThinSeparator(content);

        Table attrTable = new Table();
        strengthValueLabel = createValueLabel();
        agilityValueLabel = createValueLabel();
        wisdomValueLabel = createValueLabel();

        attrTable.add(new Label("Strength:", skin, "default")).left().padRight(15);
        attrTable.add(strengthValueLabel).right().padRight(40);
        attrTable.add(new Label("Agility:", skin, "default")).left().padRight(15);
        attrTable.add(agilityValueLabel).right().padRight(40);
        attrTable.add(new Label("Wisdom:", skin, "default")).left().padRight(15);
        attrTable.add(wisdomValueLabel).right();

        content.add(attrTable).colspan(2).padBottom(10);
        content.row();

        addSeparator(content);

        // Section: AVAILABLE POINTS
        Label pointsTitle = new Label("A V A I L A B L E   P O I N T S", skin, "title");
        pointsTitle.setColor(Color.PINK);
        content.add(pointsTitle).colspan(2).left().padBottom(8);
        content.row();
        addThinSeparator(content);

        Table pointsTable = new Table();
        attributePointsValueLabel = createValueLabel();
        skillPointsValueLabel = createValueLabel();

        pointsTable.add(new Label("Attribute Points:", skin, "default")).left().padRight(15);
        pointsTable.add(attributePointsValueLabel).right().padRight(40);
        pointsTable.add(new Label("Skill Points:", skin, "default")).left().padRight(15);
        pointsTable.add(skillPointsValueLabel).right();

        content.add(pointsTable).colspan(2).padBottom(10);
        content.row();

        addSeparator(content);

        // Close button
        TextButton closeButton = new TextButton("C L O S E", skin, "primary");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        content.add(closeButton).colspan(2).center().width(200).height(45).padTop(10);

        mainTable.add(content).fill().expand();
        window.add(mainTable).fill().expand();
        window.pack();
    }

    private Label createValueLabel() {
        Label label = new Label("0", skin, "default");
        label.setColor(Color.WHITE);
        return label;
    }

    private void addStatRow(Table table, String labelText, Label valueLabel, Color valueColor) {
        Label statLabel = new Label(labelText, skin, "default");
        statLabel.setColor(Color.LIGHT_GRAY);
        table.add(statLabel).left().padRight(20).padBottom(5);
        valueLabel.setColor(valueColor);
        table.add(valueLabel).right().padBottom(5);
        table.row();
    }

    private void addSeparator(Table table) {
        Label separator = new Label("------------------------------------------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        table.add(separator).colspan(2).center().padTop(8).padBottom(8);
        table.row();
    }

    private void addThinSeparator(Table table) {
        Label separator = new Label("-----------------------------------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        separator.setFontScale(0.7f);
        table.add(separator).colspan(2).center().padTop(3).padBottom(3);
        table.row();
    }

    public void update(Player player) {
        this.currentPlayer = player;
        if (player == null) return;

        // Progression
        levelValueLabel.setText(String.valueOf(player.getLevel()));
        int currentExp = player.getExperience();
        int nextLevelExp = player.getXpForNextLevel();
        experienceValueLabel.setText(currentExp + " / " + nextLevelExp);

        float progress = player.getXpProgress();
        experienceProgressLabel.setText(Math.round(progress * 100) + "%");
        expProgressBar.setValue(progress);

        nextLevelXpLabel.setText(nextLevelExp + " XP required");

        // Combat Status
        hpValueLabel.setText(player.getCurrentHp() + " / " + player.getMaxHp());
        manaValueLabel.setText(player.getCurrentMana() + " / " + player.getMaxMana());
        staminaValueLabel.setText(player.getCurrentStamina() + " / " + player.getMaxStamina());

        // Regen values
        hpRegenLabel.setText(player.getHpRegenPerSecond() + " hp/s");
        manaRegenLabel.setText(player.getManaRegenPerSecond() + " mp/s");
        staminaRegenLabel.setText(player.getStaminaRegenPerSecond() + " sp/s");

        // Color regen labels based on values
        hpRegenLabel.setColor(player.getHpRegenPerSecond() > 0 ? Color.GREEN : Color.RED);
        manaRegenLabel.setColor(player.getManaRegenPerSecond() > 0 ? Color.CYAN : Color.RED);
        staminaRegenLabel.setColor(player.getStaminaRegenPerSecond() > 0 ? Color.LIME : Color.RED);

        // Movement
        baseSpeedLabel.setText((int)Player.getBaseSpeed() + " px/s");
        sprintMultiplierLabel.setText("x" + Player.getSprintMultiplier());
        dashDistanceLabel.setText(Player.getDashDistance() + " px");
        dashCooldownLabel.setText((Player.getDashCooldownMs() / 1000) + "s");
        dashCostLabel.setText(Player.getDashStaminaCost() + " sp");

        baseSpeedLabel.setColor(Color.CYAN);
        sprintMultiplierLabel.setColor(Color.YELLOW);
        dashDistanceLabel.setColor(Color.WHITE);
        dashCooldownLabel.setColor(Color.WHITE);
        dashCostLabel.setColor(Color.WHITE);

        // Attributes
        strengthValueLabel.setText(String.valueOf(player.getStrength()));
        agilityValueLabel.setText(String.valueOf(player.getAgility()));
        wisdomValueLabel.setText(String.valueOf(player.getWisdom()));

        strengthValueLabel.setColor(Color.ORANGE);
        agilityValueLabel.setColor(Color.GREEN);
        wisdomValueLabel.setColor(Color.PURPLE);

        // Points
        attributePointsValueLabel.setText(String.valueOf(player.getAttributePoints()));
        skillPointsValueLabel.setText(String.valueOf(player.getSkillPoints()));

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

        // Color HP based on percentage
        float hpPercent = player.getHpPercentage();
        if (hpPercent > 0.6f) {
            hpValueLabel.setColor(Color.GREEN);
        } else if (hpPercent > 0.3f) {
            hpValueLabel.setColor(Color.ORANGE);
        } else {
            hpValueLabel.setColor(Color.RED);
        }

        // Color Mana based on percentage
        float manaPercent = player.getManaPercentage();
        if (manaPercent > 0.5f) {
            manaValueLabel.setColor(Color.CYAN);
        } else if (manaPercent > 0.2f) {
            manaValueLabel.setColor(Color.ORANGE);
        } else {
            manaValueLabel.setColor(Color.RED);
        }

        // Color Stamina based on percentage
        float staminaPercent = player.getStaminaPercentage();
        if (staminaPercent > 0.5f) {
            staminaValueLabel.setColor(Color.LIME);
        } else if (staminaPercent > 0.2f) {
            staminaValueLabel.setColor(Color.ORANGE);
        } else {
            staminaValueLabel.setColor(Color.RED);
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
        window.toFront();
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