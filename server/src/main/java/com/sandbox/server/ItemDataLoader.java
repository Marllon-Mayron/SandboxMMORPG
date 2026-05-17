package com.sandbox.server;

import com.common.sandbox.model.enums.ArmorSet;
import com.common.sandbox.model.item.ItemDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ItemDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(ItemDataLoader.class);

    private final Map<String, ItemDefinition> weapons;
    private final Map<String, ItemDefinition> armors;
    private final Map<String, ItemDefinition> consumables;
    private final Map<String, ItemDefinition> allItems;

    public ItemDataLoader() {
        this.weapons = new HashMap<>();
        this.armors = new HashMap<>();
        this.consumables = new HashMap<>();
        this.allItems = new HashMap<>();
    }

    public void loadAllItems() {
        logger.info("========================================");
        logger.info("        LOADING ITEM DEFINITIONS        ");
        logger.info("========================================");

        loadWeapons();
        loadArmors();
        loadConsumables();

        logger.info("========================================");
        logger.info("Total items loaded: {} (Weapons: {}, Armors: {}, Consumables: {})",
                allItems.size(), weapons.size(), armors.size(), consumables.size());
        logger.info("========================================");
    }

    private void loadWeapons() {
        logger.info("Loading weapons...");

        addWeapon("simple_sword", "Espada Simples", 10, 1.0f, false, 1, 0,
                "Uma espada básica de ferro forjada por aprendizes. Leve e equilibrada, ideal para iniciantes.");

        addWeapon("iron_sword", "Espada de Ferro", 18, 1.25f, false, 1, 0,
                "Espada forjada em ferro puro. Possui um bom equilíbrio entre peso e dano.");

        addWeapon("dagger", "Adaga", 6, 0.67f, false, 2, 0,
                "Uma adaga rápida e discreta. Perfeita para ataques velozes e precisos.");

        addWeapon("simple_axe", "Machado Simples", 14, 1.43f, false, 3, 0,
                "Machado de madeira com lâmina de ferro. Causa dano pesado, mas é mais lento.");

        addRangedWeapon("simple_bow", "Arco Simples", 8, 1.25f, 0, 1, 600f, 400f,
                "Arco de madeira flexível. Ideal para caça e treinamento de precisão.");

        addRangedWeapon("long_bow", "Arco Longo", 15, 1.67f, 1, 1, 900f, 550f,
                "Arco longo élfico com alcance superior. Perfeito para ataques à longa distância.");

        addRangedWeapon("quick_bow", "Arco Rápido", 6, 0.77f, 2, 1, 450f, 300f,
                "Arco curto otimizado para disparos rápidos. Sacrifica dano por velocidade.");

        logger.info("Loaded {} weapons", weapons.size());
    }

    private void addWeapon(String id, String name, int damage, float cooldown, boolean isRanged,
                           int tileX, int tileY, String description) {
        ItemDefinition def = new ItemDefinition();
        def.setId(id);
        def.setName(name);
        def.setDescription(description);
        def.setCategory("weapon");
        def.setSpritesheet("itens/spritesheet_itens.png");
        def.setTileX(tileX);
        def.setTileY(tileY);
        def.setDamage(damage);
        def.setAttackId(isRanged ? "ranged_bow" : "melee_sword");
        def.setAttackAnimation(isRanged ? "bow_shoot" : "sword_slash");
        def.setAttackCooldown(cooldown);
        def.setRanged(isRanged);
        def.setHitboxDuration(0.5f);

        if (isRanged) {
            def.setProjectileId("arrow");
            def.setProjectileSpeed(600f);
            def.setProjectileRange(400f);
            def.setProjectileAnimationId("arrow");
        } else {
            def.setProjectileAnimationId("slash");
        }

        weapons.put(id, def);
        allItems.put(id, def);
    }

    private void addRangedWeapon(String id, String name, int damage, float cooldown, int tileX, int tileY,
                                 float projectileSpeed, float projectileRange, String description) {
        ItemDefinition def = new ItemDefinition();
        def.setId(id);
        def.setName(name);
        def.setDescription(description);
        def.setCategory("weapon");
        def.setSpritesheet("itens/spritesheet_itens.png");
        def.setTileX(tileX);
        def.setTileY(tileY);
        def.setDamage(damage);
        def.setAttackId("ranged_bow");
        def.setAttackAnimation("bow_shoot");
        def.setAttackCooldown(cooldown);
        def.setRanged(true);
        def.setProjectileId("arrow");
        def.setProjectileSpeed(projectileSpeed);
        def.setProjectileRange(projectileRange);
        def.setProjectileAnimationId("arrow");
        def.setHitboxDuration(0.5f);

        weapons.put(id, def);
        allItems.put(id, def);
    }

    private void loadArmors() {
        logger.info("Loading armors...");

        addPeasantSet();
        addMageApprenticeSet();

        logger.info("Loaded {} armors", armors.size());
    }

    private void addPeasantSet() {
        // Capacete
        ItemDefinition helmet = new ItemDefinition();
        helmet.setId("peasant_helmet");
        helmet.setName("Gorro do Camponês");
        helmet.setDescription("Um gorro simples de pano, usado pelos camponeses para se proteger do sol.");
        helmet.setCategory("armor");
        helmet.setArmorSlot("helmet");  // <-- ADICIONAR
        helmet.setSpritesheet("itens/spritesheet_itens.png");
        helmet.setTileX(4);
        helmet.setTileY(0);
        helmet.setArmorSet(ArmorSet.PEASANT);
        helmet.setBonusMaxHp(20);
        helmet.setBonusHpRegen(1);
        helmet.setBonusPhysicalDefense(5);
        armors.put(helmet.getId(), helmet);
        allItems.put(helmet.getId(), helmet);

        // Peitoral
        ItemDefinition chest = new ItemDefinition();
        chest.setId("peasant_chest");
        chest.setName("Gibão de Couro Cru");
        chest.setDescription("Peitoral de couro rústico que oferece proteção básica.");
        chest.setCategory("armor");
        chest.setArmorSlot("chest");   // <-- ADICIONAR
        chest.setSpritesheet("itens/spritesheet_itens.png");
        chest.setTileX(5);
        chest.setTileY(0);
        chest.setArmorSet(ArmorSet.PEASANT);
        chest.setBonusMaxHp(30);
        chest.setBonusPhysicalDefense(10);
        chest.setBonusPhysicalPower(3);
        armors.put(chest.getId(), chest);
        allItems.put(chest.getId(), chest);

        // Calças
        ItemDefinition legs = new ItemDefinition();
        legs.setId("peasant_legs");
        legs.setName("Perneiras de Couro Firme");
        legs.setDescription("Perneiras de couro que protegem as pernas sem sacrificar a mobilidade.");
        legs.setCategory("armor");
        legs.setArmorSlot("legs");    // <-- ADICIONAR
        legs.setSpritesheet("itens/spritesheet_itens.png");
        legs.setTileX(6);
        legs.setTileY(0);
        legs.setArmorSet(ArmorSet.PEASANT);
        legs.setBonusMaxStamina(20);
        legs.setBonusPhysicalDefense(8);
        legs.setBonusMovementSpeed(15);
        armors.put(legs.getId(), legs);
        allItems.put(legs.getId(), legs);

        // Botas
        ItemDefinition boots = new ItemDefinition();
        boots.setId("peasant_boots");
        boots.setName("Alpercatas de Couro Rústico");
        boots.setDescription("Botas de couro confortáveis, ideais para longas caminhadas.");
        boots.setCategory("armor");
        boots.setArmorSlot("boots");   // <-- ADICIONAR
        boots.setSpritesheet("itens/spritesheet_itens.png");
        boots.setTileX(7);
        boots.setTileY(0);
        boots.setArmorSet(ArmorSet.PEASANT);
        boots.setBonusMaxStamina(10);
        boots.setBonusMovementSpeed(20);
        boots.setBonusStaminaRegen(2);
        armors.put(boots.getId(), boots);
        allItems.put(boots.getId(), boots);
    }

    private void addMageApprenticeSet() {
        // Capuz
        ItemDefinition hood = new ItemDefinition();
        hood.setId("apprentice_hood");
        hood.setName("Capuz do Aprendiz");
        hood.setDescription("Capuz azul que aumenta a concentração e o fluxo de mana.");
        hood.setCategory("armor");
        hood.setArmorSlot("helmet");   // <-- ADICIONAR
        hood.setSpritesheet("itens/spritesheet_itens.png");
        hood.setTileX(4);
        hood.setTileY(1);
        hood.setArmorSet(ArmorSet.MAGE_APPRENTICE);
        hood.setBonusMaxMana(25);
        hood.setBonusMagicPower(3);
        hood.setBonusCriticalChance(0.02f);
        armors.put(hood.getId(), hood);
        allItems.put(hood.getId(), hood);

        // Túnica
        ItemDefinition robe = new ItemDefinition();
        robe.setId("apprentice_robe");
        robe.setName("Túnica do Aprendiz");
        robe.setDescription("Túnica de tecido encantado que amplifica poderes arcanos.");
        robe.setCategory("armor");
        robe.setArmorSlot("chest");    // <-- ADICIONAR
        robe.setSpritesheet("itens/spritesheet_itens.png");
        robe.setTileX(5);
        robe.setTileY(1);
        robe.setArmorSet(ArmorSet.MAGE_APPRENTICE);
        robe.setBonusMaxMana(40);
        robe.setBonusMagicDefense(8);
        robe.setBonusMagicPower(5);
        robe.setBonusCooldownReduction(0.03f);
        armors.put(robe.getId(), robe);
        allItems.put(robe.getId(), robe);

        // Calças
        ItemDefinition pants = new ItemDefinition();
        pants.setId("apprentice_pants");
        pants.setName("Calças do Aprendiz");
        pants.setDescription("Calças leves que não atrapalham os movimentos durante conjurações.");
        pants.setCategory("armor");
        pants.setArmorSlot("legs");    // <-- ADICIONAR
        pants.setSpritesheet("itens/spritesheet_itens.png");
        pants.setTileX(6);
        pants.setTileY(1);
        pants.setArmorSet(ArmorSet.MAGE_APPRENTICE);
        pants.setBonusMaxMana(25);
        pants.setBonusMagicDefense(5);
        pants.setBonusMovementSpeed(10);
        armors.put(pants.getId(), pants);
        allItems.put(pants.getId(), pants);

        // Botas
        ItemDefinition boots = new ItemDefinition();
        boots.setId("apprentice_boots");
        boots.setName("Botas do Aprendiz");
        boots.setDescription("Botas macias que permitem silêncio e agilidade.");
        boots.setCategory("armor");
        boots.setArmorSlot("boots");   // <-- ADICIONAR
        boots.setSpritesheet("itens/spritesheet_itens.png");
        boots.setTileX(7);
        boots.setTileY(1);
        boots.setArmorSet(ArmorSet.MAGE_APPRENTICE);
        boots.setBonusMovementSpeed(15);
        boots.setBonusManaRegen(2);
        boots.setBonusManaSteal(0.02f);
        armors.put(boots.getId(), boots);
        allItems.put(boots.getId(), boots);
    }

    private void loadConsumables() {
        logger.info("Loading consumables...");

        ItemDefinition apple = new ItemDefinition();
        apple.setId("apple");
        apple.setName("Maçã");
        apple.setDescription("Uma maçã suculenta e crocante. Restaura 25 de HP.");
        apple.setCategory("consumable");
        apple.setSpritesheet("itens/spritesheet_itens.png");
        apple.setTileX(0);
        apple.setTileY(0);
        apple.setHealAmount(25);
        consumables.put(apple.getId(), apple);
        allItems.put(apple.getId(), apple);

        ItemDefinition healthPotion = new ItemDefinition();
        healthPotion.setId("health_potion");
        healthPotion.setName("Poção de Vida");
        healthPotion.setDescription("Poção vermelha que restaura 50 de HP instantaneamente.");
        healthPotion.setCategory("consumable");
        healthPotion.setSpritesheet("itens/spritesheet_itens.png");
        healthPotion.setTileX(3);
        healthPotion.setTileY(1);
        healthPotion.setHealAmount(50);
        consumables.put(healthPotion.getId(), healthPotion);
        allItems.put(healthPotion.getId(), healthPotion);

        logger.info("Loaded {} consumables", consumables.size());
    }

    // ==================== GETTERS ====================

    public Map<String, ItemDefinition> getWeapons() {
        return new HashMap<>(weapons);
    }

    public Map<String, ItemDefinition> getArmors() {
        return new HashMap<>(armors);
    }

    public Map<String, ItemDefinition> getConsumables() {
        return new HashMap<>(consumables);
    }

    public Map<String, ItemDefinition> getAllItems() {
        return new HashMap<>(allItems);
    }

    public ItemDefinition getItem(String id) {
        return allItems.get(id);
    }

    public boolean hasItem(String id) {
        return allItems.containsKey(id);
    }
}