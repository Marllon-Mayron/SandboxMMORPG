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
import com.common.sandbox.network.packets.player.AttributeUpgradePacket;
import com.common.sandbox.network.packets.player.PlayerStatePacket;
import com.sandbox.client.NetworkClient;
import com.sandbox.client.SandboxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AttributesWindow {
    private static final Logger logger = LoggerFactory.getLogger(AttributesWindow.class);

    private Window window;
    private Skin skin;
    private Stage stage;
    private SandboxClient gameClient;
    private NetworkClient networkClient;

    private boolean visible = false;
    private Player currentPlayer;

    private Map<String, Label> attributeValueLabels;
    private Map<String, TextButton> attributeAddButtons;
    private Map<String, Label> attributePendingLabels;
    private Map<String, Integer> pendingUpgrades;

    private Label levelValueLabel;
    private Label experienceValueLabel;
    private Label experienceProgressLabel;
    private Label attributePointsAvailableLabel;

    private ScrollPane scrollPane;
    private Table contentTable;

    private TextButton saveButton;
    private TextButton resetButton;

    private AttributeSaveCallback saveCallback;

    public interface AttributeSaveCallback {
        void onSave(Map<String, Integer> upgrades);
    }

    public AttributesWindow(Skin skin, Stage stage, SandboxClient gameClient) {
        this.skin = skin;
        this.stage = stage;
        this.gameClient = gameClient;
        this.networkClient = gameClient != null ? gameClient.getNetworkClient() : null;
        this.attributeValueLabels = new HashMap<>();
        this.attributeAddButtons = new HashMap<>();
        this.attributePendingLabels = new HashMap<>();
        this.pendingUpgrades = new HashMap<>();

        this.levelValueLabel = new Label("", skin);
        this.experienceValueLabel = new Label("", skin);
        this.experienceProgressLabel = new Label("", skin);

        createWindow();
    }

    public void setSaveCallback(AttributeSaveCallback callback) {
        this.saveCallback = callback;
    }

    private void createWindow() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.05f, 0.05f, 0.08f, 0.98f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        Drawable windowBackground = new TextureRegionDrawable(texture);

        Pixmap borderPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        borderPixmap.setColor(0.35f, 0.25f, 0.10f, 1f);
        borderPixmap.fill();
        Texture borderTexture = new Texture(borderPixmap);
        borderPixmap.dispose();
        Drawable borderDrawable = new TextureRegionDrawable(borderTexture);

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
        window = new Window("ATTRIBUTE SYSTEM", windowStyle);
        window.setModal(true);
        window.setMovable(true);
        window.setSize(600, 700);
        window.setVisible(false);

        Table mainTable = new Table();
        mainTable.setBackground(borderDrawable);

        Table rootContent = new Table();
        rootContent.pad(10);

        Table headerTable = new Table();
        headerTable.setBackground(windowBackground);
        headerTable.pad(10);

        Label headerLabel = new Label("ATTRIBUTE POINTS SYSTEM", skin, "title");
        headerLabel.setColor(Color.GOLD);
        headerLabel.setFontScale(1.2f);
        headerTable.add(headerLabel).center().colspan(2).padBottom(10);
        headerTable.row();

        attributePointsAvailableLabel = new Label("Available Points: 0", skin, "title");
        attributePointsAvailableLabel.setColor(Color.GREEN);
        headerTable.add(attributePointsAvailableLabel).center().colspan(2).padBottom(5);
        headerTable.row();

        Label infoLabel = new Label("Click + to add points, then SAVE to confirm", skin, "status");
        infoLabel.setColor(Color.LIGHT_GRAY);
        infoLabel.setFontScale(0.8f);
        headerTable.add(infoLabel).center().colspan(2).padBottom(5);

        rootContent.add(headerTable).fillX().padBottom(10);
        rootContent.row();

        contentTable = new Table();
        contentTable.pad(10);

        scrollPane = new ScrollPane(contentTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setForceScroll(false, true);

        rootContent.add(scrollPane).width(560).height(520).padBottom(10);
        rootContent.row();

        Table buttonTable = new Table();

        resetButton = new TextButton("RESET ALL", skin, "default");
        resetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                resetPendingUpgrades();
            }
        });

        saveButton = new TextButton("SAVE POINTS", skin, "primary");
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                savePendingUpgrades();
            }
        });

        TextButton closeButton = new TextButton("CLOSE", skin, "default");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });

        buttonTable.add(resetButton).width(150).height(40).padRight(10);
        buttonTable.add(saveButton).width(150).height(40).padRight(10);
        buttonTable.add(closeButton).width(150).height(40);

        rootContent.add(buttonTable).center();

        mainTable.add(rootContent).fill().expand();
        window.add(mainTable).fill().expand();

        buildAttributesTable();
        window.pack();
    }

    private void buildAttributesTable() {
        contentTable.clear();
        attributeValueLabels.clear();
        attributeAddButtons.clear();
        attributePendingLabels.clear();

        addSectionHeader("RESOURCES");
        addAttributeRow("Max HP", "max_hp", Color.GREEN);
        addAttributeRow("Max Mana", "max_mana", Color.CYAN);
        addAttributeRow("Max Stamina", "max_stamina", Color.LIME);
        addSeparator();

        addSectionHeader("REGENERATION");
        addAttributeRow("HP Regeneration", "hp_regen", Color.GREEN);
        addAttributeRow("Mana Regeneration", "mana_regen", Color.CYAN);
        addAttributeRow("Stamina Regeneration", "stamina_regen", Color.LIME);
        addSeparator();

        addSectionHeader("DEFENSES");
        addAttributeRow("Physical Defense", "physical_defense", Color.ORANGE);
        addAttributeRow("Magic Defense", "magic_defense", Color.PURPLE);
        addSeparator();

        addSectionHeader("POWER");
        addAttributeRow("Physical Power", "physical_power", Color.ORANGE);
        addAttributeRow("Ranged Power", "ranged_power", Color.GREEN);
        addAttributeRow("Magic Power", "magic_power", Color.CYAN);
        addSeparator();

        addSectionHeader("OFFENSIVE");
        addAttributeRow("Critical Chance", "critical_chance", Color.GOLD);
        addAttributeRow("Critical Damage", "critical_damage", Color.GOLD);
        addAttributeRow("Dodge Chance", "dodge_chance", Color.LIME);
        addSeparator();

        addSectionHeader("SPEEDS");
        addAttributeRow("Attack Speed", "attack_speed", Color.CYAN);
        addAttributeRow("Movement Speed", "movement_speed", Color.CYAN);
        addSeparator();

        addSectionHeader("UTILITIES");
        addAttributeRow("Cooldown Reduction", "cooldown_reduction", Color.MAGENTA);
        addAttributeRow("Life Steal", "life_steal", Color.RED);
        addAttributeRow("Mana Steal", "mana_steal", Color.BLUE);
        addAttributeRow("Tenacity", "tenacity", Color.ORANGE);
        addSeparator();

        addSectionHeader("ELEMENTAL RESISTANCES");
        addAttributeRow("Fire Resistance", "fire_resistance", Color.RED);
        addAttributeRow("Ice Resistance", "ice_resistance", Color.CYAN);
        addAttributeRow("Lightning Resistance", "lightning_resistance", Color.YELLOW);
        addAttributeRow("Poison Resistance", "poison_resistance", Color.GREEN);
        addAttributeRow("Holy Resistance", "holy_resistance", Color.GOLD);
        addAttributeRow("Dark Resistance", "dark_resistance", Color.PURPLE);
        addSeparator();

        addSectionHeader("LUCK");
        addAttributeRow("Luck (Drop Rate)", "luck", Color.GOLD);
    }

    private void addAttributeRow(String displayName, String attributeId, Color valueColor) {
        Table rowTable = new Table();

        float increment = Player.getAttributeIncrement(attributeId);
        String formattedIncrement = Player.getFormattedAttributeIncrement(attributeId);

        Label nameLabel = new Label(displayName + " " + formattedIncrement, skin, "default");
        nameLabel.setColor(Color.LIGHT_GRAY);
        nameLabel.setFontScale(0.9f);

        Label valueLabel = createValueLabel();
        valueLabel.setColor(valueColor);
        attributeValueLabels.put(attributeId, valueLabel);

        TextButton addButton = new TextButton("+", skin, "primary");
        addButton.setSize(40, 30);
        addButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                addPointToAttribute(attributeId);
            }
        });
        attributeAddButtons.put(attributeId, addButton);

        Label pendingLabel = new Label("(+0)", skin, "status");
        pendingLabel.setColor(Color.GREEN);
        pendingLabel.setFontScale(0.8f);
        attributePendingLabels.put(attributeId, pendingLabel);

        rowTable.add(nameLabel).width(220).left().padRight(10);
        rowTable.add(valueLabel).width(80).center().padRight(10);
        rowTable.add(addButton).width(40).height(30).padRight(10);
        rowTable.add(pendingLabel).width(60).left();

        contentTable.add(rowTable).left().padBottom(5).colspan(4);
        contentTable.row();
    }

    private void addSectionHeader(String title) {
        Label header = new Label(title, skin, "title");
        header.setColor(Color.CYAN);
        header.setFontScale(0.9f);
        contentTable.add(header).left().padTop(10).padBottom(5).colspan(4);
        contentTable.row();
    }

    private void addSeparator() {
        Label separator = new Label("------------------------------------------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        separator.setFontScale(0.6f);
        contentTable.add(separator).colspan(4).center().padTop(5).padBottom(5);
        contentTable.row();
    }

    private Label createValueLabel() {
        Label label = new Label("0", skin, "default");
        label.setColor(Color.WHITE);
        return label;
    }

    private void addPointToAttribute(String attributeId) {
        if (currentPlayer == null) return;

        int availablePoints = currentPlayer.getAttributePoints() - getTotalPendingPoints();
        if (availablePoints <= 0) {
            showFeedback("No attribute points available!", Color.RED);
            return;
        }

        float increment = Player.getAttributeIncrement(attributeId);
        int currentPending = pendingUpgrades.getOrDefault(attributeId, 0);

        // Adicionar o valor de incremento
        int incrementValue;
        if (increment < 1 && increment > 0) {
            // Atributos percentuais: armazenamos como pontos percentuais (1 = 1%)
            incrementValue = (int)(increment * 100);
        } else {
            incrementValue = (int)increment;
        }

        pendingUpgrades.put(attributeId, currentPending + incrementValue);

        updateAttributeDisplay(attributeId);
        updatePointsDisplay();

        showFeedback("+ Added to " + attributeId, Color.GREEN);
    }

    private void updateAttributeDisplay(String attributeId) {
        if (currentPlayer == null) return;

        Label valueLabel = attributeValueLabels.get(attributeId);
        if (valueLabel == null) return;

        int currentValue = getCurrentAttributeValue(attributeId);
        int pendingValue = pendingUpgrades.getOrDefault(attributeId, 0);
        int totalValue = currentValue + pendingValue;

        // Para atributos percentuais, mostrar como percentual
        if (isPercentAttribute(attributeId)) {
            float percentValue = totalValue / 100f;
            valueLabel.setText(String.format("%.1f%%", percentValue * 100));
        } else {
            valueLabel.setText(String.valueOf(totalValue));
        }

        if (pendingValue > 0) {
            valueLabel.setColor(Color.YELLOW);
        } else {
            valueLabel.setColor(Color.WHITE);
        }

        Label pendingLabel = attributePendingLabels.get(attributeId);
        if (pendingLabel != null) {
            if (isPercentAttribute(attributeId)) {
                float pendingPercent = pendingValue / 100f;
                pendingLabel.setText(String.format("(+%.1f%%)", pendingPercent * 100));
            } else {
                pendingLabel.setText("(+" + pendingValue + ")");
            }
            pendingLabel.setColor(pendingValue > 0 ? Color.GREEN : Color.DARK_GRAY);
        }
    }

    private boolean isPercentAttribute(String attributeId) {
        switch (attributeId) {
            case "critical_chance":
            case "critical_damage":
            case "dodge_chance":
            case "attack_speed":
            case "cooldown_reduction":
            case "life_steal":
            case "mana_steal":
            case "tenacity":
            case "fire_resistance":
            case "ice_resistance":
            case "lightning_resistance":
            case "poison_resistance":
            case "holy_resistance":
            case "dark_resistance":
                return true;
            default:
                return false;
        }
    }

    private int getPointCostForAttribute(String attributeId, int pendingValue) {
        float increment = Player.getAttributeIncrement(attributeId);
        int incrementValue = (increment < 1 && increment > 0) ? (int)(increment * 100) : (int)increment;

        if (incrementValue == 0) return pendingValue;
        return pendingValue / incrementValue;
    }

    private void savePendingUpgrades() {
        if (pendingUpgrades.isEmpty()) {
            showFeedback("No pending upgrades to save", Color.YELLOW);
            return;
        }

        int totalPointsUsed = getTotalPendingPoints();
        if (totalPointsUsed > currentPlayer.getAttributePoints()) {
            showFeedback("Not enough attribute points!", Color.RED);
            return;
        }

        logger.info("Saving {} attribute upgrades, using {} points", pendingUpgrades.size(), totalPointsUsed);

        // APLICAR OS UPGRADES LOCALMENTE PRIMEIRO
        for (Map.Entry<String, Integer> entry : pendingUpgrades.entrySet()) {
            String attributeId = entry.getKey();
            int value = entry.getValue();
            applyUpgradeToPlayer(attributeId, value);
        }

        // REMOVER OS PONTOS GASTOS
        currentPlayer.setAttributePoints(currentPlayer.getAttributePoints() - totalPointsUsed);

        // ATUALIZAR A UI IMEDIATAMENTE
        updateAllAttributesDisplay();
        updatePointsDisplay();

        // ATUALIZAR HEALTH BAR E STATUS
        if (currentPlayer.getOnStatusChanged() != null) {
            currentPlayer.getOnStatusChanged().run();
        }

        // ENVIAR PARA O SERVIDOR (SALVAR NO BANCO)
        if (networkClient != null) {
            AttributeUpgradePacket packet = new AttributeUpgradePacket();
            // Converter pendingUpgrades para o formato esperado pelo servidor (valores brutos)
            Map<String, Integer> upgradesToSend = new HashMap<>();
            for (Map.Entry<String, Integer> entry : pendingUpgrades.entrySet()) {
                String attributeId = entry.getKey();
                int value = entry.getValue();
                float increment = Player.getAttributeIncrement(attributeId);
                // Converter de volta para o valor bruto
                int rawValue;
                if (increment < 1 && increment > 0) {
                    rawValue = (int)(value / (increment * 100));
                } else {
                    rawValue = value / (int)increment;
                }
                upgradesToSend.put(attributeId, rawValue);
            }
            packet.upgrades = upgradesToSend;
            networkClient.sendPacket(packet);
        }

        // LIMPAR PENDING UPGRADES
        pendingUpgrades.clear();

        // ATUALIZAR DISPLAY NOVAMENTE PARA REMOVER OS (+) AMARELOS
        for (String attributeId : attributeValueLabels.keySet()) {
            updateAttributeDisplay(attributeId);
        }

        showFeedback("Points saved successfully!", Color.GREEN);
    }

    private void applyUpgradeToPlayer(String attributeId, int value) {
        if (currentPlayer == null) return;

        float increment = Player.getAttributeIncrement(attributeId);
        int rawValue = (increment < 1 && increment > 0) ? (int)(value / (increment * 100)) : value / (int)increment;

        switch (attributeId) {
            case "max_hp":
                currentPlayer.setBonusMaxHp(currentPlayer.getBonusMaxHp() + rawValue);
                currentPlayer.setCurrentHp(currentPlayer.getMaxHp());
                break;
            case "max_mana":
                currentPlayer.setBonusMaxMana(currentPlayer.getBonusMaxMana() + rawValue);
                currentPlayer.setCurrentMana(currentPlayer.getMaxMana());
                break;
            case "max_stamina":
                currentPlayer.setBonusMaxStamina(currentPlayer.getBonusMaxStamina() + rawValue);
                currentPlayer.setCurrentStamina(currentPlayer.getMaxStamina());
                break;
            case "hp_regen":
                currentPlayer.setBonusHpRegen(currentPlayer.getBonusHpRegen() + rawValue);
                break;
            case "mana_regen":
                currentPlayer.setBonusManaRegen(currentPlayer.getBonusManaRegen() + rawValue);
                break;
            case "stamina_regen":
                currentPlayer.setBonusStaminaRegen(currentPlayer.getBonusStaminaRegen() + rawValue);
                break;
            case "physical_defense":
                currentPlayer.setBonusPhysicalDefense(currentPlayer.getBonusPhysicalDefense() + rawValue);
                break;
            case "magic_defense":
                currentPlayer.setBonusMagicDefense(currentPlayer.getBonusMagicDefense() + rawValue);
                break;
            case "physical_power":
                currentPlayer.setBonusPhysicalPower(currentPlayer.getBonusPhysicalPower() + rawValue);
                break;
            case "ranged_power":
                currentPlayer.setBonusRangedPower(currentPlayer.getBonusRangedPower() + rawValue);
                break;
            case "magic_power":
                currentPlayer.setBonusMagicPower(currentPlayer.getBonusMagicPower() + rawValue);
                break;
            case "critical_chance":
                currentPlayer.setBonusCriticalChance(currentPlayer.getBonusCriticalChance() + (rawValue * increment));
                break;
            case "critical_damage":
                currentPlayer.setBonusCriticalDamage(currentPlayer.getBonusCriticalDamage() + (rawValue * increment));
                break;
            case "dodge_chance":
                currentPlayer.setBonusDodgeChance(currentPlayer.getBonusDodgeChance() + (rawValue * increment));
                break;
            case "attack_speed":
                currentPlayer.setBonusAttackSpeed(currentPlayer.getBonusAttackSpeed() + (rawValue * increment));
                break;
            case "movement_speed":
                currentPlayer.setBonusMovementSpeed(currentPlayer.getBonusMovementSpeed() + rawValue);
                break;
            case "cooldown_reduction":
                currentPlayer.setBonusCooldownReduction(currentPlayer.getBonusCooldownReduction() + (rawValue * increment));
                break;
            case "life_steal":
                currentPlayer.setBonusLifeSteal(currentPlayer.getBonusLifeSteal() + (rawValue * increment));
                break;
            case "mana_steal":
                currentPlayer.setBonusManaSteal(currentPlayer.getBonusManaSteal() + (rawValue * increment));
                break;
            case "tenacity":
                currentPlayer.setBonusTenacity(currentPlayer.getBonusTenacity() + (rawValue * increment));
                break;
            case "luck":
                currentPlayer.setBonusLuck(currentPlayer.getBonusLuck() + rawValue);
                break;
            case "fire_resistance":
                currentPlayer.setBonusFireResistance(currentPlayer.getBonusFireResistance() + rawValue);
                break;
            case "ice_resistance":
                currentPlayer.setBonusIceResistance(currentPlayer.getBonusIceResistance() + rawValue);
                break;
            case "lightning_resistance":
                currentPlayer.setBonusLightningResistance(currentPlayer.getBonusLightningResistance() + rawValue);
                break;
            case "poison_resistance":
                currentPlayer.setBonusPoisonResistance(currentPlayer.getBonusPoisonResistance() + rawValue);
                break;
            case "holy_resistance":
                currentPlayer.setBonusHolyResistance(currentPlayer.getBonusHolyResistance() + rawValue);
                break;
            case "dark_resistance":
                currentPlayer.setBonusDarkResistance(currentPlayer.getBonusDarkResistance() + rawValue);
                break;
        }

        currentPlayer.validateCurrentStats();
    }

    private int getCurrentAttributeValue(String attributeId) {
        if (currentPlayer == null) return 0;

        switch (attributeId) {
            case "max_hp": return currentPlayer.getBonusMaxHp();
            case "max_mana": return currentPlayer.getBonusMaxMana();
            case "max_stamina": return currentPlayer.getBonusMaxStamina();
            case "hp_regen": return currentPlayer.getBonusHpRegen();
            case "mana_regen": return currentPlayer.getBonusManaRegen();
            case "stamina_regen": return currentPlayer.getBonusStaminaRegen();
            case "physical_defense": return currentPlayer.getBonusPhysicalDefense();
            case "magic_defense": return currentPlayer.getBonusMagicDefense();
            case "physical_power": return currentPlayer.getBonusPhysicalPower();
            case "ranged_power": return currentPlayer.getBonusRangedPower();
            case "magic_power": return currentPlayer.getBonusMagicPower();
            case "critical_chance": return (int)(currentPlayer.getBonusCriticalChance() * 100);
            case "critical_damage": return (int)(currentPlayer.getBonusCriticalDamage() * 100);
            case "dodge_chance": return (int)(currentPlayer.getBonusDodgeChance() * 100);
            case "attack_speed": return (int)(currentPlayer.getBonusAttackSpeed() * 100);
            case "movement_speed": return (int)currentPlayer.getBonusMovementSpeed();
            case "cooldown_reduction": return (int)(currentPlayer.getBonusCooldownReduction() * 100);
            case "life_steal": return (int)(currentPlayer.getBonusLifeSteal() * 100);
            case "mana_steal": return (int)(currentPlayer.getBonusManaSteal() * 100);
            case "tenacity": return (int)(currentPlayer.getBonusTenacity() * 100);
            case "luck": return currentPlayer.getBonusLuck();
            case "fire_resistance": return currentPlayer.getBonusFireResistance();
            case "ice_resistance": return currentPlayer.getBonusIceResistance();
            case "lightning_resistance": return currentPlayer.getBonusLightningResistance();
            case "poison_resistance": return currentPlayer.getBonusPoisonResistance();
            case "holy_resistance": return currentPlayer.getBonusHolyResistance();
            case "dark_resistance": return currentPlayer.getBonusDarkResistance();
            default: return 0;
        }
    }

    private int getTotalPendingPoints() {
        int total = 0;
        for (Map.Entry<String, Integer> entry : pendingUpgrades.entrySet()) {
            total += getPointCostForAttribute(entry.getKey(), entry.getValue());
        }
        return total;
    }

    private void updatePointsDisplay() {
        if (currentPlayer == null) return;

        int availablePoints = currentPlayer.getAttributePoints() - getTotalPendingPoints();
        attributePointsAvailableLabel.setText("Available Points: " + availablePoints);

        if (availablePoints > 0) {
            attributePointsAvailableLabel.setColor(Color.GREEN);
        } else if (availablePoints == 0) {
            attributePointsAvailableLabel.setColor(Color.YELLOW);
        } else {
            attributePointsAvailableLabel.setColor(Color.RED);
        }

        boolean hasPoints = availablePoints > 0;
        for (TextButton button : attributeAddButtons.values()) {
            button.setDisabled(!hasPoints);
        }
    }

    private void resetPendingUpgrades() {
        pendingUpgrades.clear();

        for (String attributeId : attributeValueLabels.keySet()) {
            updateAttributeDisplay(attributeId);
        }

        updatePointsDisplay();
        showFeedback("Pending upgrades reset", Color.YELLOW);
    }

    // Este metodo sera chamado quando o servidor confirmar com PlayerStatePacket
    public void onUpgradesConfirmed() {
        // Limpar pending upgrades
        pendingUpgrades.clear();

        for (String attributeId : attributeValueLabels.keySet()) {
            updateAttributeDisplay(attributeId);
        }

        updatePointsDisplay();
        showFeedback("Points applied successfully!", Color.GREEN);
    }

    private void updateAllAttributesDisplay() {
        for (String attributeId : attributeValueLabels.keySet()) {
            updateAttributeDisplay(attributeId);
        }
    }

    private void showFeedback(String message, Color color) {
        Label feedback = new Label(message, skin, "default");
        feedback.setColor(color);
        feedback.setFontScale(0.9f);

        Table feedbackTable = new Table();
        feedbackTable.add(feedback).pad(5);

        feedbackTable.setPosition(window.getX() + window.getWidth() / 2 - 100,
                window.getY() + window.getHeight() - 30);

        stage.addActor(feedbackTable);

        com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
            @Override
            public void run() {
                feedbackTable.remove();
            }
        }, 2);
    }

    public void update(Player player) {
        this.currentPlayer = player;
        if (player == null) return;

        if (levelValueLabel != null) {
            levelValueLabel.setText(String.valueOf(player.getLevel()));
        }

        int currentExp = player.getExperience();
        int nextLevelExp = player.getXpForNextLevel();
        if (experienceValueLabel != null) {
            experienceValueLabel.setText(currentExp + " / " + nextLevelExp);
        }

        float progress = player.getXpProgress();
        if (experienceProgressLabel != null) {
            experienceProgressLabel.setText(Math.round(progress * 100) + "%");
        }

        updateAllAttributesDisplay();
        updatePointsDisplay();

        logger.info("Attributes window updated for {} - Available points: {}",
                player.getUsername(), player.getAttributePoints());
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
        resetPendingUpgrades();
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