package com.sandbox.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.common.sandbox.model.ItemDefinition;

public class ItemTooltip {
    private final Window tooltipWindow;
    private final Skin skin;
    private boolean visible = false;

    private static final int TOOLTIP_WIDTH = 360;
    private static final int ICON_SIZE = 80;
    private static final int PADDING = 15;
    private static final int OFFSET_X = 20;
    private static final int OFFSET_Y = 10;

    private Image itemIcon;
    private Label nameLabel;
    private Label typeLabel;
    private Label statsLabel;
    private Label descriptionLabel;

    public ItemTooltip(Skin skin, Stage stage) {
        this.skin = skin;
        this.tooltipWindow = createTooltipWindow();
        stage.addActor(tooltipWindow);
        tooltipWindow.setVisible(false);
    }

    private Window createTooltipWindow() {
        Window window = new Window("", skin, "default");
        window.setModal(false);
        window.setMovable(false);
        window.setSize(TOOLTIP_WIDTH, 0);
        window.setBackground(createTooltipBackground());

        Table content = new Table();
        content.pad(PADDING);

        Table topRow = new Table();

        itemIcon = new Image();
        topRow.add(itemIcon).size(ICON_SIZE, ICON_SIZE).padRight(PADDING);

        Table infoTable = new Table();
        nameLabel = new Label("", skin, "default");
        nameLabel.setFontScale(1.3f);
        nameLabel.setColor(Color.GOLD);
        nameLabel.setAlignment(Align.left);
        infoTable.add(nameLabel).left().padBottom(6);
        infoTable.row();

        typeLabel = new Label("", skin, "default");
        typeLabel.setFontScale(0.9f);
        typeLabel.setColor(Color.CYAN);
        infoTable.add(typeLabel).left();

        topRow.add(infoTable).left().expandX().fillX();
        content.add(topRow).fillX().padBottom(12);
        content.row();

        Label separator = new Label("----------------------------------------", skin, "default");
        separator.setColor(Color.DARK_GRAY);
        content.add(separator).fillX().padBottom(10);
        content.row();

        statsLabel = new Label("", skin, "default");
        statsLabel.setFontScale(0.85f);
        statsLabel.setColor(Color.LIGHT_GRAY);
        content.add(statsLabel).left().padBottom(10);
        content.row();

        descriptionLabel = new Label("", skin, "default");
        descriptionLabel.setFontScale(0.85f);
        descriptionLabel.setColor(Color.LIGHT_GRAY);
        descriptionLabel.setWrap(true);
        descriptionLabel.setAlignment(Align.topLeft);
        content.add(descriptionLabel).width(TOOLTIP_WIDTH - PADDING * 2);

        window.add(content).fill().expand();
        window.pack();

        return window;
    }

    private Drawable createTooltipBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.05f, 0.05f, 0.10f, 0.96f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        Pixmap borderPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        borderPix.setColor(0.6f, 0.5f, 0.25f, 1f);
        borderPix.fill();
        Texture borderTex = new Texture(borderPix);
        borderPix.dispose();
        borderTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        TextureRegionDrawable bg = new TextureRegionDrawable(texture);
        TextureRegionDrawable border = new TextureRegionDrawable(borderTex);

        return new Drawable() {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float x, float y, float width, float height) {
                border.draw(batch, x, y, width, height);
                bg.draw(batch, x + 3, y + 3, width - 6, height - 6);
            }
            @Override public float getLeftWidth() { return 3; }
            @Override public void setLeftWidth(float leftWidth) {}
            @Override public float getRightWidth() { return 3; }
            @Override public void setRightWidth(float rightWidth) {}
            @Override public float getTopHeight() { return 3; }
            @Override public void setTopHeight(float topHeight) {}
            @Override public float getBottomHeight() { return 3; }
            @Override public void setBottomHeight(float bottomHeight) {}
            @Override public float getMinWidth() { return 0; }
            @Override public void setMinWidth(float minWidth) {}
            @Override public float getMinHeight() { return 0; }
            @Override public void setMinHeight(float minHeight) {}
        };
    }

    public void show(ItemDefinition item, TextureRegion icon, float mouseX, float mouseY) {
        if (item == null) return;

        if (icon != null) {
            itemIcon.setDrawable(new TextureRegionDrawable(icon));
        } else {
            Pixmap pixmap = new Pixmap(ICON_SIZE, ICON_SIZE, Pixmap.Format.RGBA8888);
            pixmap.setColor(0.2f, 0.2f, 0.3f, 1f);
            pixmap.fill();
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            itemIcon.setDrawable(new TextureRegionDrawable(texture));
        }

        nameLabel.setText(item.getName());

        Color nameColor = getRarityColor(item);
        nameLabel.setColor(nameColor);

        typeLabel.setText(getCategoryDisplayName(item.getCategory()));

        StringBuilder stats = new StringBuilder();
        if ("weapon".equals(item.getCategory())) {
            stats.append("Damage: ").append(item.getDamage());
            if (item.isRanged()) {
                stats.append(" | Ranged");
            } else {
                stats.append(" | Melee");
            }
            stats.append("\nAttack Speed: ").append(String.format("%.1f", item.getAttackSpeed()));
            stats.append(" | Cooldown: ").append(String.format("%.1f sec", item.getAttackCooldown()));
        } else if ("consumable".equals(item.getCategory())) {
            stats.append("Heal: ").append(item.getHealAmount());
        }

        if (item.getStrengthBonus() > 0) stats.append("\nStrength +").append(item.getStrengthBonus());
        if (item.getAgilityBonus() > 0) stats.append("\nAgility +").append(item.getAgilityBonus());
        if (item.getWisdomBonus() > 0) stats.append("\nWisdom +").append(item.getWisdomBonus());

        statsLabel.setText(stats.toString());

        String description = getItemDescription(item);
        descriptionLabel.setText(description);

        tooltipWindow.pack();

        float tooltipX = mouseX + OFFSET_X;
        float tooltipY = mouseY - OFFSET_Y;

        Stage stage = tooltipWindow.getStage();
        if (stage != null) {
            if (tooltipX + tooltipWindow.getWidth() > stage.getWidth()) {
                tooltipX = mouseX - tooltipWindow.getWidth() - OFFSET_X;
            }
            if (tooltipY - tooltipWindow.getHeight() < 0) {
                tooltipY = mouseY + OFFSET_Y;
            }
            tooltipX = Math.max(5, Math.min(tooltipX, stage.getWidth() - tooltipWindow.getWidth() - 5));
            tooltipY = Math.max(5, Math.min(tooltipY, stage.getHeight() - 5));
        }

        tooltipWindow.setPosition(tooltipX, tooltipY);
        tooltipWindow.setVisible(true);
        tooltipWindow.toFront();
        visible = true;
    }

    public void hide() {
        tooltipWindow.setVisible(false);
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void toFront() {
        if (tooltipWindow != null) {
            tooltipWindow.toFront();
        }
    }

    private Color getRarityColor(ItemDefinition item) {
        if ("weapon".equals(item.getCategory())) {
            if (item.getDamage() >= 15) return Color.GOLD;
            if (item.getDamage() >= 10) return new Color(0.7f, 0.4f, 0.8f, 1f);
            return Color.WHITE;
        } else if ("consumable".equals(item.getCategory())) {
            return new Color(0.3f, 0.8f, 0.3f, 1f);
        }
        return Color.WHITE;
    }

    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "weapon": return "Weapon";
            case "consumable": return "Consumable";
            case "armor": return "Armor";
            case "quest": return "Quest Item";
            default: return "Item";
        }
    }

    private String getItemDescription(ItemDefinition item) {
        if ("weapon".equals(item.getCategory())) {
            if (item.isRanged()) {
                return "A ranged weapon that fires projectiles at enemies from a distance. Effective against targets from afar.";
            } else {
                return "A melee weapon for close combat engagement. Effective at short range.";
            }
        } else if ("consumable".equals(item.getCategory())) {
            return "Restores " + item.getHealAmount() + " health points when consumed. Can be used in combat or out of combat.";
        }
        return "A mysterious item of unknown origin and purpose. Its properties are yet to be discovered.";
    }

    public void dispose() {
        if (tooltipWindow != null) {
            tooltipWindow.clear();
        }
    }
}