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
import com.common.sandbox.model.Inventory;
import com.common.sandbox.model.ItemDefinition;
import com.common.sandbox.model.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryWindow {
    private static final Logger logger = LoggerFactory.getLogger(InventoryWindow.class);

    private static final int SLOT_SIZE = 64;
    private static final int SLOT_PADDING = 2;
    private static final int INVENTORY_COLUMNS = 5;
    private static final int WINDOW_WIDTH = 680;
    private static final int WINDOW_HEIGHT = 580;

    private final Window window;
    private final Skin skin;
    private final Stage stage;
    private final DragAndDrop dragAndDrop;

    private final Map<Integer, InventorySlot> inventorySlots;
    private final Map<String, EquipmentSlot> equipmentSlots;
    private final Map<String, TextureRegion> itemTextures;
    private final Map<String, ItemDefinition> itemDefinitions;

    private Table inventoryTable;
    private Table equipmentTable;
    private Label goldLabel;

    private Inventory currentInventory;
    private boolean visible;

    private TextureRegion defaultIcon;
    private TextureRegion emptySlotIcon;

    private Callbacks callbacks;

    public InventoryWindow(Skin skin, Stage stage) {
        this.skin = skin;
        this.stage = stage;
        this.inventorySlots = new ConcurrentHashMap<>();
        this.equipmentSlots = new ConcurrentHashMap<>();
        this.itemTextures = new ConcurrentHashMap<>();
        this.itemDefinitions = new ConcurrentHashMap<>();
        this.dragAndDrop = new DragAndDrop();
        this.callbacks = new Callbacks();

        initializeIcons();
        this.window = createWindow();
        setupDragAndDrop();
    }

    private void initializeIcons() {
        this.defaultIcon = createDefaultIcon();
        this.emptySlotIcon = createEmptySlotIcon();
    }

    private TextureRegion createDefaultIcon() {
        Pixmap pixmap = new Pixmap(SLOT_SIZE, SLOT_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.2f, 0.2f, 0.25f, 0.9f);
        pixmap.fill();

        pixmap.setColor(0.5f, 0.5f, 0.6f, 1f);
        for (int i = 0; i < SLOT_SIZE; i++) {
            pixmap.drawPixel(i, 0);
            pixmap.drawPixel(i, SLOT_SIZE - 1);
            pixmap.drawPixel(0, i);
            pixmap.drawPixel(SLOT_SIZE - 1, i);
        }

        pixmap.setColor(0.7f, 0.7f, 0.8f, 1f);
        int centerX = SLOT_SIZE / 2;
        int centerY = SLOT_SIZE / 2;
        for (int i = -8; i <= 8; i++) {
            pixmap.drawPixel(centerX + i, centerY + 4);
        }
        for (int i = -6; i <= 6; i++) {
            pixmap.drawPixel(centerX + i, centerY + 2);
            pixmap.drawPixel(centerX + i, centerY + 6);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }

    private TextureRegion createEmptySlotIcon() {
        Pixmap pixmap = new Pixmap(SLOT_SIZE, SLOT_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.08f, 0.08f, 0.12f, 0.6f);
        pixmap.fill();

        pixmap.setColor(0.2f, 0.2f, 0.3f, 0.8f);
        for (int i = 0; i < SLOT_SIZE; i++) {
            pixmap.drawPixel(i, 0);
            pixmap.drawPixel(i, SLOT_SIZE - 1);
            pixmap.drawPixel(0, i);
            pixmap.drawPixel(SLOT_SIZE - 1, i);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }

    private Window createWindow() {
        Window win = new Window("INVENTORY", skin, "default");
        win.setModal(false);
        win.setMovable(true);
        win.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        win.setVisible(false);

        Table mainContent = new Table();
        mainContent.pad(10);

        mainContent.add(createHeader()).width(WINDOW_WIDTH - 40).padBottom(10);
        mainContent.row();

        mainContent.add(createEquipmentSection()).width(WINDOW_WIDTH - 40).padBottom(15);
        mainContent.row();

        mainContent.add(createSeparator()).padBottom(10);
        mainContent.row();

        mainContent.add(createInventorySection()).width(WINDOW_WIDTH - 40).padBottom(10);
        mainContent.row();

        mainContent.add(createHintLabel()).center();

        win.add(mainContent).fill().expand();
        win.pack();

        return win;
    }

    private Table createHeader() {
        Table header = new Table();

        Label titleLabel = new Label("INVENTORY", skin, "title");
        titleLabel.setColor(Color.GOLD);

        goldLabel = new Label("Gold: 0", skin, "default");
        goldLabel.setColor(Color.YELLOW);

        header.add(titleLabel).left().expandX();
        header.add(goldLabel).right();

        return header;
    }

    private Table createEquipmentSection() {
        Table section = new Table();

        Label equipLabel = new Label("EQUIPMENT", skin, "title");
        equipLabel.setColor(Color.CYAN);
        equipLabel.setFontScale(0.9f);

        equipmentTable = new Table();
        equipmentTable.setBackground(createSlotBackground());
        equipmentTable.pad(5);

        addEquipmentSlot("weapon", "Weapon", 0);
        addEquipmentSlot("helmet", "Helmet", 1);
        addEquipmentSlot("chest", "Chest", 2);
        addEquipmentSlot("legs", "Legs", 3);
        addEquipmentSlot("boots", "Boots", 4);

        section.add(equipLabel).left().padBottom(5);
        section.row();
        section.add(equipmentTable);

        return section;
    }

    private Table createInventorySection() {
        Table section = new Table();

        Label invLabel = new Label("INVENTORY", skin, "title");
        invLabel.setColor(Color.ORANGE);
        invLabel.setFontScale(0.9f);

        inventoryTable = new Table();
        inventoryTable.setBackground(createSlotBackground());
        inventoryTable.pad(5);

        for (int slot = 0; slot < Inventory.TOTAL_SLOTS; slot++) {
            InventorySlot inventorySlot = new InventorySlot(slot);
            inventorySlots.put(slot, inventorySlot);
            inventoryTable.add(inventorySlot.getContainer()).size(SLOT_SIZE, SLOT_SIZE).pad(SLOT_PADDING);

            if ((slot + 1) % INVENTORY_COLUMNS == 0) {
                inventoryTable.row();
            }
        }

        section.add(invLabel).left().padBottom(5);
        section.row();
        section.add(inventoryTable);

        return section;
    }

    private Label createSeparator() {
        Label separator = new Label("--------------------------------------------------", skin, "status");
        separator.setColor(Color.DARK_GRAY);
        return separator;
    }

    private Label createHintLabel() {
        Label hintLabel = new Label("Drag & Drop to move | Right click to drop | Double click to equip/unequip", skin, "status");
        hintLabel.setFontScale(0.7f);
        hintLabel.setColor(Color.LIGHT_GRAY);
        return hintLabel;
    }

    private void addEquipmentSlot(String slotType, String label, int index) {
        EquipmentSlot eqSlot = new EquipmentSlot(slotType, label);
        equipmentSlots.put(slotType, eqSlot);
        equipmentTable.add(eqSlot.getContainer()).size(SLOT_SIZE, SLOT_SIZE).pad(SLOT_PADDING);

        if (index == 4) {
            equipmentTable.row();
        }
    }

    private Drawable createSlotBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.08f, 0.08f, 0.12f, 0.85f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        Pixmap borderPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        borderPix.setColor(0.35f, 0.25f, 0.10f, 1f);
        borderPix.fill();
        Texture borderTex = new Texture(borderPix);
        borderPix.dispose();

        TextureRegionDrawable bg = new TextureRegionDrawable(texture);
        TextureRegionDrawable border = new TextureRegionDrawable(borderTex);

        return new Drawable() {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float x, float y, float width, float height) {
                border.draw(batch, x, y, width, height);
                bg.draw(batch, x + 2, y + 2, width - 4, height - 4);
            }
            @Override public float getLeftWidth() { return 2; }
            @Override public void setLeftWidth(float leftWidth) {}
            @Override public float getRightWidth() { return 2; }
            @Override public void setRightWidth(float rightWidth) {}
            @Override public float getTopHeight() { return 2; }
            @Override public void setTopHeight(float topHeight) {}
            @Override public float getBottomHeight() { return 2; }
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
            table.add(new Image(region)).size(48, 48);
        } else {
            Label label = new Label("?", skin);
            label.setColor(Color.WHITE);
            table.add(label).size(48, 48);
        }

        table.setSize(52, 52);
        return table;
    }

    public void registerItemTexture(String itemId, TextureRegion region, ItemDefinition definition) {
        if (itemId == null || definition == null) {
            logger.warn("Cannot register item with null ID or definition");
            return;
        }

        logger.info("Registering item in InventoryWindow - ID: {}, Name: {}, Category: {}",
                itemId, definition.getName(), definition.getCategory());

        itemDefinitions.put(itemId, definition);
        itemTextures.put(itemId, region != null ? region : defaultIcon);

        if (currentInventory != null && visible) {
            refreshDisplay();
        }
    }

    public boolean isItemRegistered(String itemId) {
        if (itemId == null) return false;
        return itemDefinitions.containsKey(itemId);
    }

    private boolean isEquippable(String itemId) {
        ItemDefinition def = itemDefinitions.get(itemId);
        if (def == null) return false;

        String category = def.getCategory();
        return "weapon".equals(category) || "armor".equals(category) || "equipment".equals(category);
    }

    private String getEquipmentSlotForItem(String itemId) {
        ItemDefinition def = itemDefinitions.get(itemId);
        if (def == null) return null;

        String category = def.getCategory();

        switch (category) {
            case "weapon": return "weapon";
            case "armor":
                if (itemId.contains("helmet")) return "helmet";
                if (itemId.contains("chest")) return "chest";
                if (itemId.contains("legs")) return "legs";
                if (itemId.contains("boots")) return "boots";
                return "chest";
            case "equipment": return "weapon";
            default: return null;
        }
    }

    private TextureRegion getItemTexture(String itemId) {
        return itemTextures.getOrDefault(itemId, defaultIcon);
    }

    private String getItemName(String itemId) {
        ItemDefinition def = itemDefinitions.get(itemId);
        return def != null ? def.getName() : itemId;
    }

    public void updateInventory(Inventory inventory, int gold) {
        this.currentInventory = inventory;

        if (goldLabel != null) {
            goldLabel.setText("Gold: " + gold);
        }

        refreshDisplay();
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
    }

    public void hide() {
        visible = false;
        window.setVisible(false);
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

    public void showDropConfirmation(String itemName, int quantity, Runnable onConfirm) {
        Dialog dialog = new Dialog("Drop Item", skin) {
            @Override
            protected void result(Object object) {
                if ((Boolean) object && onConfirm != null) {
                    onConfirm.run();
                }
            }
        };

        dialog.text("Drop " + quantity + "x " + itemName + "?");
        dialog.button("Yes", true);
        dialog.button("No", false);
        dialog.show(stage);
    }

    public void setOnMoveItem(MoveItemCallback callback) { callbacks.moveItem = callback; }
    public void setOnEquip(EquipItemCallback callback) { callbacks.equipItem = callback; }
    public void setOnUnequip(UnequipItemCallback callback) { callbacks.unequipItem = callback; }
    public void setOnDrop(DropItemCallback callback) { callbacks.dropItem = callback; }

    public void dispose() {
        window.clear();
        if (defaultIcon != null && defaultIcon.getTexture() != null) {
            defaultIcon.getTexture().dispose();
        }
        if (emptySlotIcon != null && emptySlotIcon.getTexture() != null) {
            emptySlotIcon.getTexture().dispose();
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
            this.nameLabel = new Label("", skin, "default");
            this.quantityLabel = new Label("", skin, "default");

            nameLabel.setFontScale(0.55f);
            nameLabel.setColor(Color.WHITE);
            nameLabel.setAlignment(Align.center);

            quantityLabel.setFontScale(0.7f);
            quantityLabel.setColor(Color.YELLOW);
            quantityLabel.setAlignment(Align.bottomRight);

            setupLayout();
            setupListeners();
        }

        private void setupLayout() {
            container.clear();
            container.setBackground(createSlotBackground());

            // Stack para sobrepor os elementos
            Stack stack = new Stack();

            // Camada 1: Imagem do item (fundo)
            stack.add(itemImage);

            // Camada 2: Table com nome e quantidade sobrepostos
            Table overlay = new Table();
            overlay.top().left();
            overlay.add(nameLabel).padTop(4).padLeft(4).expandX().left();
            overlay.row();
            overlay.add().expandY();
            overlay.row();
            overlay.add(quantityLabel).padBottom(4).padRight(4).expandX().right();

            stack.add(overlay);

            container.add(stack).fill().expand();
        }

        private void setupListeners() {
            container.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (isDoubleClick()) {
                        handleDoubleClick();
                    }
                    lastClickTime = System.currentTimeMillis();
                }

                private boolean isDoubleClick() {
                    long currentTime = System.currentTimeMillis();
                    return currentTime - lastClickTime < 500;
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
        }

        Table getContainer() {
            return container;
        }

        void update(ItemStack stack) {
            boolean hasItem = stack != null && !stack.isEmpty();

            if (hasItem) {
                String itemId = stack.getItemId();
                TextureRegion icon = getItemTexture(itemId);
                itemImage.setDrawable(new TextureRegionDrawable(icon != null ? icon : defaultIcon));

                String itemName = getItemName(itemId);
                String displayName = itemName.length() > 10 ? itemName.substring(0, 8) + "..." : itemName;
                nameLabel.setText(displayName);
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
        private long lastClickTime;

        EquipmentSlot(String slotType, String label) {
            this.slotType = slotType;
            this.label = label;
            this.container = new Table();
            this.itemImage = new Image();
            this.itemLabel = new Label("", skin, "default");

            itemLabel.setFontScale(0.55f);
            itemLabel.setColor(Color.CYAN);
            itemLabel.setAlignment(Align.center);

            setupLayout();
            setupListeners();
        }

        private void setupLayout() {
            container.clear();
            container.setBackground(createSlotBackground());

            Stack stack = new Stack();
            stack.add(itemImage);

            Table overlay = new Table();
            overlay.center();
            overlay.add(itemLabel).center();

            stack.add(overlay);
            container.add(stack).fill().expand();
        }

        private void setupListeners() {
            container.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (isDoubleClick()) {
                        handleDoubleClick();
                    }
                    lastClickTime = System.currentTimeMillis();
                }

                private boolean isDoubleClick() {
                    long currentTime = System.currentTimeMillis();
                    return currentTime - lastClickTime < 500;
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
                        default: return 0;
                    }
                }
            });
        }

        Table getContainer() {
            return container;
        }

        void update(String itemId) {
            boolean hasItem = itemId != null && !itemId.isEmpty();

            if (hasItem) {
                TextureRegion icon = getItemTexture(itemId);
                itemImage.setDrawable(new TextureRegionDrawable(icon != null ? icon : defaultIcon));

                String itemName = getItemName(itemId);
                String displayName = itemName.length() > 12 ? itemName.substring(0, 10) + "..." : itemName;
                itemLabel.setText(displayName);
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

        InventoryDragSource(InventorySlot slot) {
            super(slot.getContainer());
            this.slot = slot;
        }

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

        InventoryDragTarget(InventorySlot slot) {
            super(slot.getContainer());
            this.slot = slot;
        }

        @Override
        public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
            return true;
        }

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

        EquipmentDragSource(EquipmentSlot slot) {
            super(slot.getContainer());
            this.slot = slot;
        }

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

        EquipmentDragTarget(EquipmentSlot slot) {
            super(slot.getContainer());
            this.slot = slot;
        }

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
        OutsideWindowTarget(Actor actor) {
            super(actor);
        }

        @Override
        public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
            return true;
        }

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

    @FunctionalInterface
    public interface MoveItemCallback {
        void accept(int fromSlot, int toSlot);
    }

    @FunctionalInterface
    public interface EquipItemCallback {
        void accept(int inventorySlot, String equipmentSlot);
    }

    @FunctionalInterface
    public interface UnequipItemCallback {
        void accept(int equipmentIndex);
    }

    @FunctionalInterface
    public interface DropItemCallback {
        void accept(DropAction action);
    }
}