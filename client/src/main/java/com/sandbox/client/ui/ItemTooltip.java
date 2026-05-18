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
import com.common.sandbox.model.item.ItemDefinition;

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
        descriptionLabel.setColor(new Color(0.8f, 0.8f, 0.9f, 1f));
        descriptionLabel.setWrap(true);
        descriptionLabel.setAlignment(Align.topLeft);
        content.add(descriptionLabel).width(TOOLTIP_WIDTH - PADDING * 2).padTop(8);

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

        // Bônus de recursos
        if (item.getBonusMaxHp() > 0) stats.append("\n+").append(item.getBonusMaxHp()).append(" Max HP");
        if (item.getBonusMaxMana() > 0) stats.append("\n+").append(item.getBonusMaxMana()).append(" Max Mana");
        if (item.getBonusMaxStamina() > 0) stats.append("\n+").append(item.getBonusMaxStamina()).append(" Max Stamina");

        // Bônus de regeneração
        if (item.getBonusHpRegen() > 0) stats.append("\n+").append(item.getBonusHpRegen()).append(" HP Regen");
        if (item.getBonusManaRegen() > 0) stats.append("\n+").append(item.getBonusManaRegen()).append(" Mana Regen");
        if (item.getBonusStaminaRegen() > 0) stats.append("\n+").append(item.getBonusStaminaRegen()).append(" Stamina Regen");

        // Bônus de defesa
        if (item.getBonusPhysicalDefense() > 0) stats.append("\n+").append(item.getBonusPhysicalDefense()).append(" Physical Defense");
        if (item.getBonusMagicDefense() > 0) stats.append("\n+").append(item.getBonusMagicDefense()).append(" Magic Defense");

        // Bônus de poder
        if (item.getBonusPhysicalPower() > 0) stats.append("\n+").append(item.getBonusPhysicalPower()).append(" Physical Power");
        if (item.getBonusRangedPower() > 0) stats.append("\n+").append(item.getBonusRangedPower()).append(" Ranged Power");
        if (item.getBonusMagicPower() > 0) stats.append("\n+").append(item.getBonusMagicPower()).append(" Magic Power");

        // Bônus de chance
        if (item.getBonusCriticalChance() > 0) stats.append("\n+").append((int)(item.getBonusCriticalChance() * 100)).append("% Critical Chance");
        if (item.getBonusCriticalDamage() > 0) stats.append("\n+").append((int)(item.getBonusCriticalDamage() * 100)).append("% Critical Damage");
        if (item.getBonusDodgeChance() > 0) stats.append("\n+").append((int)(item.getBonusDodgeChance() * 100)).append("% Dodge Chance");

        // Bônus de velocidade
        if (item.getBonusAttackSpeed() > 0) stats.append("\n+").append((int)(item.getBonusAttackSpeed() * 100)).append("% Attack Speed");
        if (item.getBonusMovementSpeed() > 0) stats.append("\n+").append((int)item.getBonusMovementSpeed()).append(" Movement Speed");

        // Bônus de utilidades
        if (item.getBonusCooldownReduction() > 0) stats.append("\n+").append((int)(item.getBonusCooldownReduction() * 100)).append("% Cooldown Reduction");
        if (item.getBonusLifeSteal() > 0) stats.append("\n+").append((int)(item.getBonusLifeSteal() * 100)).append("% Life Steal");
        if (item.getBonusManaSteal() > 0) stats.append("\n+").append((int)(item.getBonusManaSteal() * 100)).append("% Mana Steal");
        if (item.getBonusTenacity() > 0) stats.append("\n+").append((int)(item.getBonusTenacity() * 100)).append("% Tenacity");

        // Sorte
        if (item.getBonusLuck() > 0) stats.append("\n+").append(item.getBonusLuck()).append(" Luck");

        // Resistências elementais
        if (item.getBonusFireResistance() > 0) stats.append("\n+").append(item.getBonusFireResistance()).append("% Fire Resistance");
        if (item.getBonusIceResistance() > 0) stats.append("\n+").append(item.getBonusIceResistance()).append("% Ice Resistance");
        if (item.getBonusLightningResistance() > 0) stats.append("\n+").append(item.getBonusLightningResistance()).append("% Lightning Resistance");
        if (item.getBonusPoisonResistance() > 0) stats.append("\n+").append(item.getBonusPoisonResistance()).append("% Poison Resistance");
        if (item.getBonusHolyResistance() > 0) stats.append("\n+").append(item.getBonusHolyResistance()).append("% Holy Resistance");
        if (item.getBonusDarkResistance() > 0) stats.append("\n+").append(item.getBonusDarkResistance()).append("% Dark Resistance");

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
        } else if ("armor".equals(item.getCategory())) {
            // Armaduras com bônus mais altos tem cor diferente
            if (item.getBonusMaxHp() > 30 || item.getBonusPhysicalDefense() > 15) {
                return new Color(0.7f, 0.4f, 0.8f, 1f); // Roxo para raras
            }
            if (item.getBonusMaxHp() > 0 || item.getBonusPhysicalDefense() > 0) {
                return new Color(0.3f, 0.6f, 0.9f, 1f); // Azul para mágicas
            }
            return Color.WHITE;
        } else if ("accessory".equals(item.getCategory())) {
            // Acessórios têm cor dourada
            return new Color(0.9f, 0.7f, 0.2f, 1f); // Dourado
        }
        return Color.WHITE;
    }

    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "weapon": return "Weapon";
            case "consumable": return "Consumable";
            case "armor": return "Armor";
            case "accessory": return "Accessory";
            case "quest": return "Quest Item";
            default: return "Item";
        }
    }

    private String getItemDescription(ItemDefinition item) {
        // Se o item tem descrição personalizada, usa ela
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            return item.getDescription();
        }

        // Fallback para descrições genéricas
        if ("weapon".equals(item.getCategory())) {
            if (item.isRanged()) {
                return "Uma arma de longo alcance que dispara projéteis contra inimigos à distância.";
            } else {
                return "Uma arma corpo a corpo para combate próximo.";
            }
        } else if ("consumable".equals(item.getCategory())) {
            return "Restaura " + item.getHealAmount() + " pontos de vida quando consumido.";
        } else if ("armor".equals(item.getCategory())) {
            String slot = item.getArmorSlot();
            if (slot != null) {
                switch (slot) {
                    case "helmet": return "Um capacete que protege a cabeça e concede bônus de atributos.";
                    case "chest": return "Uma armadura de peito que oferece proteção e bônus de atributos.";
                    case "legs": return "Perneiras que protegem as pernas e concedem bônus de atributos.";
                    case "boots": return "Botas que protegem os pés e concedem bônus de velocidade.";
                    default: return "Uma peça de armadura que oferece proteção e bônus de atributos.";
                }
            }
            return "Uma peça de armadura que oferece proteção e bônus de atributos.";
        } else if ("accessory".equals(item.getCategory())) {
            String slot = item.getAccessorySlot();
            if (slot != null) {
                switch (slot) {
                    case "ring": return "Um anel mágico que concede bônus de atributos.";
                    case "necklace": return "Um colar encantado que concede bônus de atributos.";
                    case "cloak": return "Uma capa que aumenta agilidade e concede bônus de esquiva.";
                    case "trinket": return "Um acessório misterioso que concede bônus especiais.";
                    default: return "Um acessório que concede bônus de atributos.";
                }
            }
            return "Um acessório que concede bônus de atributos.";
        }

        return "Um item misterioso de origem desconhecida.";
    }

    public void dispose() {
        if (tooltipWindow != null) {
            tooltipWindow.clear();
        }
    }
}