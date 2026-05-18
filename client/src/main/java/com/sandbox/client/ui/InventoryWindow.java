package com.sandbox.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.common.sandbox.model.enums.Rarity;
import com.common.sandbox.model.item.Inventory;
import com.common.sandbox.model.item.ItemDefinition;
import com.common.sandbox.model.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryWindow {
    private static final Logger logger = LoggerFactory.getLogger(InventoryWindow.class);

    private static final int SLOT_SIZE = 88;
    private static final int SLOT_PADDING = 4;
    private static final int INVENTORY_COLUMNS = 6;
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 650;

    private final Window window;
    private final Skin skin;
    private final Stage stage;
    private final DragAndDrop dragAndDrop;
    private final ItemTooltip itemTooltip;

    private final Map<Integer, InventorySlot> inventorySlots;
    private final Map<String, EquipmentSlot> equipmentSlots;
    private final Map<String, TextureRegion> itemTextures;
    private final Map<String, ItemDefinition> itemDefinitions;

    private Table inventoryTable;
    private Table equipmentTable;
    private Label goldLabel;
    private Label weightLabel;
    private Label statsLabel;

    private Inventory currentInventory;
    private boolean visible;

    private TextureRegion defaultIcon;
    private TextureRegion emptySlotIcon;
    private TextureRegion panelBackground;
    private TextureRegion titleDecoration;

    private Callbacks callbacks;

    // Cores por raridade
    private static final Map<Rarity, Color> RARITY_COLORS = new ConcurrentHashMap<>();
    static {
        RARITY_COLORS.put(Rarity.COMMON, new Color(0.8f, 0.8f, 0.8f, 1f));      // Branco
        RARITY_COLORS.put(Rarity.UNCOMMON, new Color(0.3f, 0.8f, 0.3f, 1f));    // Verde
        RARITY_COLORS.put(Rarity.RARE, new Color(0.3f, 0.5f, 0.9f, 1f));        // Azul
        RARITY_COLORS.put(Rarity.EPIC, new Color(0.7f, 0.3f, 0.8f, 1f));        // Roxo
        RARITY_COLORS.put(Rarity.LEGENDARY, new Color(0.9f, 0.7f, 0.2f, 1f));   // Dourado
        RARITY_COLORS.put(Rarity.MYTHIC, new Color(0.9f, 0.3f, 0.5f, 1f));      // Vermelho/Rosa
    }

    public InventoryWindow(Skin skin, Stage stage) {
        this.skin = skin;
        this.stage = stage;
        this.inventorySlots = new ConcurrentHashMap<>();
        this.equipmentSlots = new ConcurrentHashMap<>();
        this.itemTextures = new ConcurrentHashMap<>();
        this.itemDefinitions = new ConcurrentHashMap<>();
        this.dragAndDrop = new DragAndDrop();
        this.callbacks = new Callbacks();
        this.itemTooltip = new ItemTooltip(skin, stage);

        initializeIcons();
        createCustomSkin();
        this.window = createWindow();
        setupDragAndDrop();
    }

    private void initializeIcons() {
        this.defaultIcon = createDefaultIcon();
        this.emptySlotIcon = createEmptySlotIcon();
        this.panelBackground = createPanelBackground();
        this.titleDecoration = createTitleDecoration();
    }

    private TextureRegion createDefaultIcon() {
        Pixmap pixmap = new Pixmap(SLOT_SIZE, SLOT_SIZE, Pixmap.Format.RGBA8888);
        for (int y = 0; y < SLOT_SIZE; y++) {
            float ratio = (float) y / SLOT_SIZE;
            int r = (int)(30 + ratio * 20);
            int g = (int)(25 + ratio * 15);
            int b = (int)(40 + ratio * 25);
            pixmap.setColor(r/255f, g/255f, b/255f, 0.95f);
            pixmap.drawLine(0, y, SLOT_SIZE - 1, y);
        }

        pixmap.setColor(0.85f, 0.65f, 0.25f, 1f);
        for (int i = 0; i < SLOT_SIZE; i++) {
            pixmap.drawPixel(i, 0);
            pixmap.drawPixel(i, SLOT_SIZE - 1);
            pixmap.drawPixel(0, i);
            pixmap.drawPixel(SLOT_SIZE - 1, i);
        }

        pixmap.setColor(0.6f, 0.45f, 0.15f, 1f);
        for (int i = 1; i < SLOT_SIZE - 1; i++) {
            pixmap.drawPixel(i, 1);
            pixmap.drawPixel(i, SLOT_SIZE - 2);
            pixmap.drawPixel(1, i);
            pixmap.drawPixel(SLOT_SIZE - 2, i);
        }

        pixmap.setColor(0.7f, 0.6f, 0.4f, 1f);
        int centerX = SLOT_SIZE / 2;
        int centerY = SLOT_SIZE / 2;
        for (int i = -12; i <= 12; i++) {
            pixmap.drawPixel(centerX + i, centerY + 6);
        }
        for (int i = -8; i <= 8; i++) {
            pixmap.drawPixel(centerX + i, centerY + 8);
            pixmap.drawPixel(centerX + i, centerY + 10);
        }
        pixmap.setColor(0.9f, 0.8f, 0.5f, 1f);
        for (int i = -4; i <= 4; i++) {
            pixmap.drawPixel(centerX + i, centerY + 9);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return new TextureRegion(texture);
    }

    private TextureRegion createEmptySlotIcon() {
        Pixmap pixmap = new Pixmap(SLOT_SIZE, SLOT_SIZE, Pixmap.Format.RGBA8888);
        for (int y = 0; y < SLOT_SIZE; y++) {
            float ratio = (float) y / SLOT_SIZE;
            int a = (int)(40 + ratio * 30);
            pixmap.setColor(0.08f, 0.08f, 0.12f, a/255f);
            pixmap.drawLine(0, y, SLOT_SIZE - 1, y);
        }

        pixmap.setColor(0.25f, 0.25f, 0.35f, 0.8f);
        for (int i = 0; i < SLOT_SIZE; i++) {
            pixmap.drawPixel(i, 0);
            pixmap.drawPixel(i, SLOT_SIZE - 1);
            pixmap.drawPixel(0, i);
            pixmap.drawPixel(SLOT_SIZE - 1, i);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return new TextureRegion(texture);
    }

    private TextureRegion createPanelBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.06f, 0.06f, 0.10f, 0.92f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return new TextureRegion(texture);
    }

    private TextureRegion createTitleDecoration() {
        Pixmap pixmap = new Pixmap(200, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.85f, 0.65f, 0.25f, 1f);
        for (int i = 0; i < 4; i++) {
            pixmap.drawLine(0, i, 199, i);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return new TextureRegion(texture);
    }

    private void createCustomSkin() {
        Label.LabelStyle largeTitleStyle = new Label.LabelStyle();
        largeTitleStyle.font = skin.getFont("default-font");
        largeTitleStyle.fontColor = Color.GOLD;
        skin.add("large-title", largeTitleStyle);

        Label.LabelStyle statsStyle = new Label.LabelStyle();
        statsStyle.font = skin.getFont("default-font");
        statsStyle.fontColor = Color.LIGHT_GRAY;
        skin.add("stats", statsStyle);
    }

    private Window createWindow() {
        Window win = new Window("", skin, "default");
        win.setModal(false);
        win.setMovable(true);
        win.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        win.setVisible(false);
        win.setBackground(new TextureRegionDrawable(panelBackground));

        Table mainContent = new Table();
        mainContent.pad(15);

        mainContent.add(createHeader()).width(WINDOW_WIDTH - 40).padBottom(15);
        mainContent.row();

        Table body = new Table();
        body.add(createEquipmentSection()).width(280).padRight(20);
        body.add(createInventorySection()).width(540);
        mainContent.add(body).padBottom(15);
        mainContent.row();

        mainContent.add(createFooter()).width(WINDOW_WIDTH - 40).padTop(10);

        win.add(mainContent).fill().expand();
        win.pack();

        return win;
    }

    private Table createHeader() {
        Table header = new Table();

        Label titleLabel = new Label("I N V E N T O R Y", skin, "large-title");
        titleLabel.setFontScale(1.4f);
        titleLabel.setColor(Color.GOLD);

        Table titleDecor = new Table();
        titleDecor.add(new Image(titleDecoration)).width(180).height(4);
        titleDecor.row();
        titleDecor.add(titleLabel).padTop(8);
        titleDecor.row();
        titleDecor.add(new Image(titleDecoration)).width(180).height(4).padTop(8);

        goldLabel = new Label("Gold: 0", skin, "stats");
        goldLabel.setFontScale(0.9f);
        goldLabel.setColor(Color.YELLOW);

        weightLabel = new Label("Weight: 0/100", skin, "stats");
        weightLabel.setFontScale(0.9f);
        weightLabel.setColor(Color.CYAN);

        header.add(titleDecor).center().expandX();
        header.row();
        header.add(goldLabel).right().padTop(5);
        header.row();
        header.add(weightLabel).right();

        return header;
    }

    private Table createEquipmentSection() {
        Table section = new Table();

        Label equipLabel = new Label("E Q U I P M E N T", skin, "large-title");
        equipLabel.setFontScale(0.85f);
        equipLabel.setColor(Color.CYAN);

        equipmentTable = new Table();
        equipmentTable.setBackground(createSlotBackground());
        equipmentTable.pad(10);

        // Primeira linha: Arma e Capacete
        addEquipmentSlot("weapon", "Weapon");
        addEquipmentSlot("helmet", "Helmet");
        equipmentTable.row();

        // Segunda linha: Peitoral
        addEquipmentSlot("chest", "Chest");
        equipmentTable.row();

        // Terceira linha: Calças e Botas
        addEquipmentSlot("legs", "Legs");
        addEquipmentSlot("boots", "Boots");
        equipmentTable.row();

        addSeparatorLine(equipmentTable);

        // Quarta linha: Anel 1 e Anel 2
        addEquipmentSlot("ring1", "Ring 1");
        addEquipmentSlot("ring2", "Ring 2");
        equipmentTable.row();

        // Quinta linha: Colar e Capa
        addEquipmentSlot("necklace", "Necklace");
        addEquipmentSlot("cloak", "Cloak");
        equipmentTable.row();

        addSeparatorLine(equipmentTable);

        // Sexta linha: Acessórios (Trinkets)
        addEquipmentSlot("trinket1", "Trinket 1");
        addEquipmentSlot("trinket2", "Trinket 2");
        equipmentTable.row();
        addEquipmentSlot("trinket3", "Trinket 3");
        equipmentTable.row();

        section.add(equipLabel).center().padBottom(12);
        section.row();
        section.add(equipmentTable).padBottom(10);
        section.row();

        // Stats container
        statsLabel = new Label("", skin, "stats");
        statsLabel.setFontScale(0.85f);
        statsLabel.setColor(Color.LIGHT_GRAY);
        statsLabel.setWrap(true);
        statsLabel.setAlignment(Align.center);

        Table statsContainer = new Table();
        statsContainer.setBackground(createSlotBackground());
        statsContainer.pad(8);
        statsContainer.add(statsLabel).width(260).pad(5);

        section.add(statsContainer).center().padTop(8).width(280);
        section.row();

        Label separator = new Label("────────────────────", skin, "stats");
        separator.setColor(Color.DARK_GRAY);
        separator.setFontScale(0.6f);
        section.add(separator).center().padTop(5);

        return section;
    }

    private void addSeparatorLine(Table table) {
        Label separator = new Label("──────────────────", skin, "stats");
        separator.setColor(Color.DARK_GRAY);
        separator.setFontScale(0.5f);
        table.add(separator).colspan(2).center().padTop(8).padBottom(8);
        table.row();
    }

    private Table createInventorySection() {
        Table section = new Table();

        Label invLabel = new Label("I N V E N T O R Y", skin, "large-title");
        invLabel.setFontScale(0.85f);
        invLabel.setColor(Color.ORANGE);

        inventoryTable = new Table();
        inventoryTable.setBackground(createSlotBackground());
        inventoryTable.pad(8);

        for (int slot = 0; slot < Inventory.TOTAL_SLOTS; slot++) {
            InventorySlot inventorySlot = new InventorySlot(slot);
            inventorySlots.put(slot, inventorySlot);
            // Aumentar o padding vertical para mais espaçamento entre linhas
            inventoryTable.add(inventorySlot.getContainer())
                    .size(SLOT_SIZE, SLOT_SIZE + 24)  // Altura maior para acomodar o nome
                    .pad(SLOT_PADDING);

            if ((slot + 1) % INVENTORY_COLUMNS == 0) {
                inventoryTable.row();
                // Adicionar espaçamento extra entre linhas
                inventoryTable.add().height(4);
                inventoryTable.row();
            }
        }

        section.add(invLabel).center().padBottom(12);
        section.row();
        section.add(inventoryTable);

        return section;
    }

    private Table createFooter() {
        Table footer = new Table();

        Label hintLabel = new Label("Drag and Drop to move | Right click to drop | Double click to equip/unequip", skin, "stats");
        hintLabel.setFontScale(0.65f);
        hintLabel.setColor(Color.LIGHT_GRAY);

        footer.add(hintLabel).center();

        return footer;
    }

    private void addEquipmentSlot(String slotType, String label) {
        EquipmentSlot eqSlot = new EquipmentSlot(slotType, label);
        equipmentSlots.put(slotType, eqSlot);
        equipmentTable.add(eqSlot.getContainer()).size(SLOT_SIZE, SLOT_SIZE).pad(SLOT_PADDING);
    }

    private Drawable createSlotBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.10f, 0.10f, 0.15f, 0.90f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        Pixmap borderPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        borderPix.setColor(0.55f, 0.45f, 0.20f, 1f);
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

    private void setupDragAndDrop() {
        for (InventorySlot slot : inventorySlots.values()) {
            dragAndDrop.addSource(new InventoryDragSource(slot));
            dragAndDrop.addTarget(new InventoryDragTarget(slot));
        }

        for (EquipmentSlot eqSlot : equipmentSlots.values()) {
            dragAndDrop.addSource(new EquipmentDragSource(eqSlot));
            dragAndDrop.addTarget(new EquipmentDragTarget(eqSlot));
        }

        dragAndDrop.addTarget(new OutsideWindowTarget(window));
    }

    private Actor createDragActor(String itemId) {
        Table table = new Table();
        table.setBackground(createSlotBackground());

        TextureRegion region = getItemTexture(itemId);
        if (region != null) {
            table.add(new Image(region)).size(56, 56);
        } else {
            Label label = new Label("?", skin);
            label.setColor(Color.WHITE);
            label.setFontScale(1.2f);
            table.add(label).size(56, 56);
        }

        table.setSize(62, 62);
        return table;
    }

    private void showTooltip(ItemDefinition item, TextureRegion icon, Actor actor, InputEvent event) {
        if (item == null || itemTooltip == null) return;

        float mouseX = event.getStageX();
        float mouseY = event.getStageY();

        itemTooltip.show(item, icon, mouseX, mouseY);
    }

    private void hideTooltip() {
        if (itemTooltip != null) {
            itemTooltip.hide();
        }
    }

    // Método para obter cor baseada na raridade do item
    private Color getRarityColor(ItemDefinition item) {
        if (item == null) return Color.WHITE;
        Rarity rarity = item.getRarity();
        Color color = RARITY_COLORS.get(rarity);
        return color != null ? color : Color.WHITE;
    }

    // Método para obter string da raridade
    private String getRarityDisplayName(ItemDefinition item) {
        if (item == null) return "";
        Rarity rarity = item.getRarity();
        switch (rarity) {
            case COMMON: return "COMMON";
            case UNCOMMON: return "UNCOMMON";
            case RARE: return "RARE";
            case EPIC: return "EPIC";
            case LEGENDARY: return "LEGENDARY";
            case MYTHIC: return "MYTHIC";
            default: return "";
        }
    }

    public void registerItemTexture(String itemId, TextureRegion region, ItemDefinition definition) {
        if (itemId == null || definition == null) {
            logger.warn("Cannot register item with null ID or definition");
            return;
        }

        logger.info("Registering item in InventoryWindow - ID: {}, Name: {}, Category: {}, Rarity: {}",
                itemId, definition.getName(), definition.getCategory(), definition.getRarity());

        itemDefinitions.put(itemId, definition);
        itemTextures.put(itemId, region != null ? region : defaultIcon);

        if (currentInventory != null && visible) {
            refreshDisplay();
        }
    }

    private TextureRegion getItemTexture(String itemId) {
        return itemTextures.getOrDefault(itemId, defaultIcon);
    }

    private String getItemName(String itemId) {
        ItemDefinition def = itemDefinitions.get(itemId);
        return def != null ? def.getName() : itemId;
    }

    private ItemDefinition getItemDefinition(String itemId) {
        return itemDefinitions.get(itemId);
    }

    private boolean isEquippable(String itemId) {
        ItemDefinition def = itemDefinitions.get(itemId);
        if (def == null) return false;
        String category = def.getCategory();
        return "weapon".equals(category) || "armor".equals(category) || "accessory".equals(category) || "equipment".equals(category);
    }

    private String getEquipmentSlotForItem(String itemId) {
        ItemDefinition def = itemDefinitions.get(itemId);
        if (def == null) return null;

        String category = def.getCategory();

        if ("weapon".equals(category)) {
            return Inventory.SLOT_WEAPON;
        }

        if ("armor".equals(category)) {
            String armorSlot = def.getArmorSlot();
            if (armorSlot != null) {
                return armorSlot;
            }
            return null;
        }

        if ("accessory".equals(category)) {
            String accessorySlot = def.getAccessorySlot();
            if (accessorySlot != null) {
                // Para anéis, podemos alternar entre ring1 e ring2
                if ("ring".equals(accessorySlot)) {
                    // Verificar qual slot está vazio
                    String currentRing1 = currentInventory.getEquipped().get(Inventory.SLOT_RING_1);
                    String currentRing2 = currentInventory.getEquipped().get(Inventory.SLOT_RING_2);
                    if (currentRing1 == null || currentRing1.isEmpty()) {
                        return Inventory.SLOT_RING_1;
                    } else if (currentRing2 == null || currentRing2.isEmpty()) {
                        return Inventory.SLOT_RING_2;
                    }
                    // Ambos ocupados, substituir o primeiro
                    return Inventory.SLOT_RING_1;
                }
                return accessorySlot;
            }
            return null;
        }

        return null;
    }

    public void updateInventory(Inventory inventory, int gold) {
        this.currentInventory = inventory;

        if (goldLabel != null) {
            goldLabel.setText("Gold: " + gold);
        }

        int totalWeight = 0;
        if (inventory != null) {
            for (ItemStack stack : inventory.getSlots().values()) {
                if (stack != null && !stack.isEmpty()) {
                    totalWeight += stack.getQuantity();
                }
            }
        }
        if (weightLabel != null) {
            weightLabel.setText("Weight: " + totalWeight + "/100");
        }

        updateStatsLabel();
        refreshDisplay();
    }

    private void updateStatsLabel() {
        if (statsLabel == null || currentInventory == null) return;

        StringBuilder stats = new StringBuilder();
        int totalMaxHp = 0;
        int totalMaxMana = 0;
        int totalMaxStamina = 0;
        int totalPhysicalPower = 0;
        int totalRangedPower = 0;
        int totalMagicPower = 0;
        int totalPhysicalDefense = 0;
        int totalMagicDefense = 0;
        float totalCriticalChance = 0;
        float totalMovementSpeed = 0;
        float totalCooldownReduction = 0;
        float totalAttackSpeed = 0;
        float totalLifeSteal = 0;
        float totalManaSteal = 0;
        float totalDodgeChance = 0;
        int totalLuck = 0;

        for (String itemId : currentInventory.getEquipped().values()) {
            if (itemId != null && !itemId.isEmpty()) {
                ItemDefinition def = itemDefinitions.get(itemId);
                if (def != null) {
                    totalMaxHp += def.getBonusMaxHp();
                    totalMaxMana += def.getBonusMaxMana();
                    totalMaxStamina += def.getBonusMaxStamina();
                    totalPhysicalPower += def.getBonusPhysicalPower();
                    totalRangedPower += def.getBonusRangedPower();
                    totalMagicPower += def.getBonusMagicPower();
                    totalPhysicalDefense += def.getBonusPhysicalDefense();
                    totalMagicDefense += def.getBonusMagicDefense();
                    totalCriticalChance += def.getBonusCriticalChance();
                    totalMovementSpeed += def.getBonusMovementSpeed();
                    totalCooldownReduction += def.getBonusCooldownReduction();
                    totalAttackSpeed += def.getBonusAttackSpeed();
                    totalLifeSteal += def.getBonusLifeSteal();
                    totalManaSteal += def.getBonusManaSteal();
                    totalDodgeChance += def.getBonusDodgeChance();
                    totalLuck += def.getBonusLuck();
                }
            }
        }

        // Estilo com formatação e cores
        stats.append("[ EQUIPMENT BONUS ]\n");

        // Recursos
        if (totalMaxHp > 0) stats.append("  HP: +").append(totalMaxHp).append("\n");
        if (totalMaxMana > 0) stats.append("  Mana: +").append(totalMaxMana).append("\n");
        if (totalMaxStamina > 0) stats.append("  Stamina: +").append(totalMaxStamina).append("\n");

        // Poder de Dano
        if (totalPhysicalPower > 0) stats.append("  Physical Power: +").append(totalPhysicalPower).append("\n");
        if (totalRangedPower > 0) stats.append("  Ranged Power: +").append(totalRangedPower).append("\n");
        if (totalMagicPower > 0) stats.append("  Magic Power: +").append(totalMagicPower).append("\n");

        // Defesas
        if (totalPhysicalDefense > 0) stats.append("  Physical Defense: +").append(totalPhysicalDefense).append("\n");
        if (totalMagicDefense > 0) stats.append("  Magic Defense: +").append(totalMagicDefense).append("\n");

        // Chance e multiplicadores
        if (totalCriticalChance > 0) stats.append("  Critical Chance: +").append((int)(totalCriticalChance * 100)).append("%\n");
        if (totalDodgeChance > 0) stats.append("  Dodge Chance: +").append((int)(totalDodgeChance * 100)).append("%\n");

        // Velocidades
        if (totalAttackSpeed > 0) stats.append("  Attack Speed: +").append((int)(totalAttackSpeed * 100)).append("%\n");
        if (totalMovementSpeed > 0) stats.append("  Movement Speed: +").append((int)totalMovementSpeed).append("\n");

        // Utilidades
        if (totalCooldownReduction > 0) stats.append("  Cooldown Reduction: +").append((int)(totalCooldownReduction * 100)).append("%\n");
        if (totalLifeSteal > 0) stats.append("  Life Steal: +").append((int)(totalLifeSteal * 100)).append("%\n");
        if (totalManaSteal > 0) stats.append("  Mana Steal: +").append((int)(totalManaSteal * 100)).append("%\n");

        // Sorte
        if (totalLuck > 0) stats.append("  Luck: +").append(totalLuck).append("\n");

        // Se não houver nenhum bônus
        if (stats.toString().equals("[ EQUIPMENT BONUS ]\n")) {
            stats.append("  No active bonuses");
        }

        statsLabel.setText(stats.toString());

        // Aplicar cores baseadas nos valores
        if (totalMaxHp > 0 || totalPhysicalDefense > 0) {
            statsLabel.setColor(Color.GREEN);
        } else if (totalPhysicalPower > 0) {
            statsLabel.setColor(Color.ORANGE);
        } else if (totalMagicPower > 0) {
            statsLabel.setColor(Color.CYAN);
        } else {
            statsLabel.setColor(Color.LIGHT_GRAY);
        }
    }

    private void refreshDisplay() {
        if (currentInventory == null) return;

        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            InventorySlot slot = inventorySlots.get(i);
            if (slot != null) {
                slot.update(currentInventory.getSlot(i));
            }
        }

        for (EquipmentSlot eqSlot : equipmentSlots.values()) {
            String itemId = currentInventory.getEquipped().get(eqSlot.slotType);
            eqSlot.update(itemId);
        }

        updateStatsLabel();
        window.invalidate();
        window.pack();
    }

    public void show() {
        visible = true;
        window.setVisible(true);
        if (stage != null && window.getParent() == null) {
            stage.addActor(window);
        }
        window.toFront();
        centerPosition(stage.getWidth(), stage.getHeight());
    }

    public void hide() {
        visible = false;
        window.setVisible(false);
        hideTooltip();
    }

    public void toggle() {
        if (visible) hide();
        else show();
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

    public boolean isItemRegistered(String itemId) {
        return itemDefinitions.containsKey(itemId);
    }

    public void setOnMoveItem(MoveItemCallback callback) {
        this.callbacks.moveItem = callback;
    }

    public void setOnEquip(EquipItemCallback callback) {
        this.callbacks.equipItem = callback;
    }

    public void setOnUnequip(UnequipItemCallback callback) {
        this.callbacks.unequipItem = callback;
    }

    public void setOnDrop(DropItemCallback callback) {
        this.callbacks.dropItem = callback;
    }

    public void dispose() {
        window.clear();
        if (defaultIcon != null && defaultIcon.getTexture() != null) {
            defaultIcon.getTexture().dispose();
        }
        if (emptySlotIcon != null && emptySlotIcon.getTexture() != null) {
            emptySlotIcon.getTexture().dispose();
        }
        if (panelBackground != null && panelBackground.getTexture() != null) {
            panelBackground.getTexture().dispose();
        }
        if (titleDecoration != null && titleDecoration.getTexture() != null) {
            titleDecoration.getTexture().dispose();
        }
        if (itemTooltip != null) {
            itemTooltip.dispose();
        }
    }

    // ==================== INNER CLASSES ====================

    private class InventorySlot {
        final int slot;
        final Table container;
        final Image itemImage;
        final Label nameLabel;
        final Label quantityLabel;
        private long lastClickTime;

        InventorySlot(int slot) {
            this.slot = slot;
            this.container = new Table();
            this.itemImage = new Image();
            this.nameLabel = new Label("", skin, "stats");
            this.quantityLabel = new Label("", skin, "stats");

            nameLabel.setFontScale(0.65f);
            nameLabel.setColor(Color.WHITE);
            nameLabel.setAlignment(Align.center);
            nameLabel.setWrap(true);

            quantityLabel.setFontScale(0.8f);
            quantityLabel.setColor(Color.YELLOW);
            quantityLabel.setAlignment(Align.topRight);

            setupLayout();
            setupListeners();
        }

        private void setupLayout() {
            container.clear();

            // Layout vertical com mais espaçamento
            Table verticalLayout = new Table();
            verticalLayout.center();
            verticalLayout.pad(6, 0, 6, 0); // Padding vertical (cima, baixo)

            // Nome do item (em cima)
            verticalLayout.add(nameLabel).width(SLOT_SIZE).padBottom(6);
            verticalLayout.row();

            // Imagem do item com a quantidade sobreposta
            Table imageContainer = new Table();
            imageContainer.setBackground(createSlotBackground());

            Stack imageStack = new Stack();

            // Imagem do item
            imageStack.add(itemImage);

            // Quantidade no canto superior direito
            Table quantityOverlay = new Table();
            quantityOverlay.top().right();
            quantityOverlay.add(quantityLabel).padTop(4).padRight(6);
            imageStack.add(quantityOverlay);

            imageContainer.add(imageStack).size(SLOT_SIZE - 8, SLOT_SIZE - 8);
            verticalLayout.add(imageContainer).size(SLOT_SIZE, SLOT_SIZE).padBottom(4);

            container.add(verticalLayout).fill().expand();
        }

        private void setupListeners() {
            container.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (getButton() == 1) {
                        handleRightClick();
                    } else if (isDoubleClick()) {
                        handleDoubleClick();
                    }
                    lastClickTime = System.currentTimeMillis();
                }

                private boolean isDoubleClick() {
                    long currentTime = System.currentTimeMillis();
                    return currentTime - lastClickTime < 500;
                }

                private void handleRightClick() {
                    if (callbacks.dropItem != null && currentInventory != null) {
                        ItemStack stack = currentInventory.getSlot(slot);
                        if (stack != null && !stack.isEmpty()) {
                            callbacks.dropItem.accept(new DropAction(slot, stack.getQuantity()));
                        }
                    }
                }

                private void handleDoubleClick() {
                    if (callbacks.equipItem != null && currentInventory != null) {
                        ItemStack stack = currentInventory.getSlot(slot);
                        if (stack != null && !stack.isEmpty() && isEquippable(stack.getItemId())) {
                            String equipSlot = getEquipmentSlotForItem(stack.getItemId());
                            if (equipSlot != null) {
                                callbacks.equipItem.accept(slot, equipSlot);
                            }
                        }
                    }
                }
            });

            container.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (currentInventory != null) {
                        ItemStack stack = currentInventory.getSlot(slot);
                        if (stack != null && !stack.isEmpty()) {
                            ItemDefinition def = itemDefinitions.get(stack.getItemId());
                            if (def != null) {
                                TextureRegion icon = getItemTexture(stack.getItemId());
                                showTooltip(def, icon, container, event);
                            }
                        }
                    }
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    hideTooltip();
                }
            });
        }

        Table getContainer() { return container; }

        void update(ItemStack stack) {
            boolean hasItem = stack != null && !stack.isEmpty();

            if (hasItem) {
                String itemId = stack.getItemId();
                TextureRegion icon = getItemTexture(itemId);
                itemImage.setDrawable(new TextureRegionDrawable(icon != null ? icon : defaultIcon));

                String itemName = getItemName(itemId);
                String displayName = itemName.length() > 12 ? itemName.substring(0, 10) + ".." : itemName;
                nameLabel.setText(displayName);

                // Aplicar cor do nome baseada na raridade do item
                ItemDefinition def = getItemDefinition(itemId);
                if (def != null) {
                    nameLabel.setColor(getRarityColor(def));
                } else {
                    nameLabel.setColor(Color.WHITE);
                }

                nameLabel.setVisible(true);

                if (stack.getQuantity() > 1) {
                    quantityLabel.setText(String.valueOf(stack.getQuantity()));
                    quantityLabel.setVisible(true);
                } else {
                    quantityLabel.setText("");
                    quantityLabel.setVisible(false);
                }
            } else {
                itemImage.setDrawable(new TextureRegionDrawable(emptySlotIcon));
                nameLabel.setText("");
                nameLabel.setVisible(false);
                quantityLabel.setText("");
                quantityLabel.setVisible(false);
            }

            container.invalidate();
        }
    }

    private class EquipmentSlot {
        final String slotType;
        final String label;
        final Table container;
        final Image itemImage;
        final Label itemLabel;
        final Label slotLabel;
        private long lastClickTime;

        EquipmentSlot(String slotType, String label) {
            this.slotType = slotType;
            this.label = label;
            this.container = new Table();
            this.itemImage = new Image();
            this.itemLabel = new Label("", skin, "stats");
            this.slotLabel = new Label(label, skin, "stats");

            itemLabel.setFontScale(0.65f);
            itemLabel.setColor(Color.CYAN);
            itemLabel.setAlignment(Align.center);
            itemLabel.setWrap(true);

            slotLabel.setFontScale(0.65f);
            slotLabel.setColor(Color.GRAY);
            slotLabel.setAlignment(Align.center);

            setupLayout();
            setupListeners();
        }

        private void setupLayout() {
            container.clear();

            // Layout vertical: nome em cima, imagem no meio, slot label embaixo
            Table verticalLayout = new Table();
            verticalLayout.center();

            // Nome do item equipado (em cima)
            verticalLayout.add(itemLabel).width(SLOT_SIZE).padBottom(4);
            verticalLayout.row();

            // Imagem do item (centro)
            Table imageContainer = new Table();
            imageContainer.setBackground(createSlotBackground());
            imageContainer.add(itemImage).size(SLOT_SIZE - 8, SLOT_SIZE - 8);
            verticalLayout.add(imageContainer).size(SLOT_SIZE, SLOT_SIZE);
            verticalLayout.row();

            // Nome do slot (embaixo)
            verticalLayout.add(slotLabel).width(SLOT_SIZE).padTop(4);

            container.add(verticalLayout).fill().expand();
        }

        private void setupListeners() {
            container.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (getButton() == 1) {
                        handleRightClick();
                    } else if (isDoubleClick()) {
                        handleDoubleClick();
                    }
                    lastClickTime = System.currentTimeMillis();
                }

                private boolean isDoubleClick() {
                    long currentTime = System.currentTimeMillis();
                    return currentTime - lastClickTime < 500;
                }

                private void handleRightClick() {
                    if (callbacks.unequipItem != null && currentInventory != null) {
                        String itemId = currentInventory.getEquipped().get(slotType);
                        if (itemId != null && !itemId.isEmpty()) {
                            callbacks.unequipItem.accept(getSlotIndex());
                        }
                    }
                }

                private void handleDoubleClick() {
                    if (callbacks.unequipItem != null && currentInventory != null) {
                        String itemId = currentInventory.getEquipped().get(slotType);
                        if (itemId != null && !itemId.isEmpty()) {
                            callbacks.unequipItem.accept(getSlotIndex());
                        }
                    }
                }

                private int getSlotIndex() {
                    switch (slotType) {
                        case "weapon": return 0;
                        case "helmet": return 1;
                        case "chest": return 2;
                        case "legs": return 3;
                        case "boots": return 4;
                        case "ring1": return 5;
                        case "ring2": return 6;
                        case "necklace": return 7;
                        case "cloak": return 8;
                        case "trinket1": return 9;
                        case "trinket2": return 10;
                        case "trinket3": return 11;
                        default: return 0;
                    }
                }
            });

            container.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (currentInventory != null) {
                        String itemId = currentInventory.getEquipped().get(slotType);
                        if (itemId != null && !itemId.isEmpty()) {
                            ItemDefinition def = itemDefinitions.get(itemId);
                            if (def != null) {
                                TextureRegion icon = getItemTexture(itemId);
                                showTooltip(def, icon, container, event);
                            }
                        }
                    }
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    hideTooltip();
                }
            });
        }

        Table getContainer() { return container; }

        void update(String itemId) {
            boolean hasItem = itemId != null && !itemId.isEmpty();

            if (hasItem) {
                TextureRegion icon = getItemTexture(itemId);
                itemImage.setDrawable(new TextureRegionDrawable(icon != null ? icon : defaultIcon));

                String itemName = getItemName(itemId);
                String displayName = itemName.length() > 12 ? itemName.substring(0, 10) + ".." : itemName;
                itemLabel.setText(displayName);

                // Aplicar cor do nome baseada na raridade do item
                ItemDefinition def = getItemDefinition(itemId);
                if (def != null) {
                    itemLabel.setColor(getRarityColor(def));
                } else {
                    itemLabel.setColor(Color.CYAN);
                }

                itemLabel.setVisible(true);
            } else {
                itemImage.setDrawable(new TextureRegionDrawable(emptySlotIcon));
                itemLabel.setText("");
                itemLabel.setVisible(false);
            }

            container.invalidate();
        }
    }

    private class InventoryDragSource extends Source {
        final InventorySlot slot;
        InventoryDragSource(InventorySlot slot) { super(slot.getContainer()); this.slot = slot; }
        @Override
        public Payload dragStart(InputEvent event, float x, float y, int pointer) {
            if (currentInventory == null) return null;
            ItemStack stack = currentInventory.getSlot(slot.slot);
            if (stack == null || stack.isEmpty()) return null;
            Payload payload = new Payload();
            payload.setObject(new DragData(slot.slot, true, stack.getItemId()));
            payload.setDragActor(createDragActor(stack.getItemId()));
            return payload;
        }
    }

    private class InventoryDragTarget extends Target {
        final InventorySlot slot;
        InventoryDragTarget(InventorySlot slot) { super(slot.getContainer()); this.slot = slot; }
        @Override
        public boolean drag(Source source, Payload payload, float x, float y, int pointer) { return true; }
        @Override
        public void drop(Source source, Payload payload, float x, float y, int pointer) {
            DragData data = (DragData) payload.getObject();
            if (data == null || !data.isInventory) return;
            if (callbacks.moveItem != null && data.slot != slot.slot) {
                callbacks.moveItem.accept(data.slot, slot.slot);
            }
        }
    }

    private class EquipmentDragSource extends Source {
        final EquipmentSlot slot;
        EquipmentDragSource(EquipmentSlot slot) { super(slot.getContainer()); this.slot = slot; }
        @Override
        public Payload dragStart(InputEvent event, float x, float y, int pointer) {
            if (currentInventory == null) return null;
            String itemId = currentInventory.getEquipped().get(slot.slotType);
            if (itemId == null || itemId.isEmpty()) return null;
            Payload payload = new Payload();
            payload.setObject(new DragData(-1, false, itemId));
            payload.setDragActor(createDragActor(itemId));
            return payload;
        }
    }

    private class EquipmentDragTarget extends Target {
        final EquipmentSlot slot;
        EquipmentDragTarget(EquipmentSlot slot) { super(slot.getContainer()); this.slot = slot; }
        @Override
        public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
            DragData data = (DragData) payload.getObject();
            if (data == null || !data.isInventory) return false;
            if (!isEquippable(data.itemId)) return false;
            String expectedSlot = getEquipmentSlotForItem(data.itemId);
            return expectedSlot != null && expectedSlot.equals(slot.slotType);
        }
        @Override
        public void drop(Source source, Payload payload, float x, float y, int pointer) {
            DragData data = (DragData) payload.getObject();
            if (data == null || !data.isInventory) return;
            if (callbacks.equipItem != null && isEquippable(data.itemId)) {
                callbacks.equipItem.accept(data.slot, slot.slotType);
            }
        }
    }

    private class OutsideWindowTarget extends Target {
        OutsideWindowTarget(Actor actor) { super(actor); }
        @Override
        public boolean drag(Source source, Payload payload, float x, float y, int pointer) { return true; }
        @Override
        public void drop(Source source, Payload payload, float x, float y, int pointer) {
            DragData data = (DragData) payload.getObject();
            if (data == null || !data.isInventory || callbacks.dropItem == null || currentInventory == null) return;
            ItemStack stack = currentInventory.getSlot(data.slot);
            if (stack != null && !stack.isEmpty()) {
                callbacks.dropItem.accept(new DropAction(data.slot, stack.getQuantity()));
            }
        }
    }

    private static class DragData {
        final int slot;
        final boolean isInventory;
        final String itemId;
        DragData(int slot, boolean isInventory, String itemId) {
            this.slot = slot;
            this.isInventory = isInventory;
            this.itemId = itemId;
        }
    }

    public static class DropAction {
        public final int slot;
        public final int quantity;
        public DropAction(int slot, int quantity) {
            this.slot = slot;
            this.quantity = quantity;
        }
    }

    private static class Callbacks {
        MoveItemCallback moveItem;
        EquipItemCallback equipItem;
        UnequipItemCallback unequipItem;
        DropItemCallback dropItem;
    }

    @FunctionalInterface public interface MoveItemCallback { void accept(int fromSlot, int toSlot); }
    @FunctionalInterface public interface EquipItemCallback { void accept(int inventorySlot, String equipmentSlot); }
    @FunctionalInterface public interface UnequipItemCallback { void accept(int equipmentIndex); }
    @FunctionalInterface public interface DropItemCallback { void accept(DropAction action); }
}