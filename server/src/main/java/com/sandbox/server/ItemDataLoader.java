package com.sandbox.server;

import com.common.sandbox.model.enums.AnimationType;
import com.common.sandbox.model.enums.ArmorSet;
import com.common.sandbox.model.enums.Rarity;
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
    private final Map<String, ItemDefinition> accessories;
    private final Map<String, ItemDefinition> allItems;

    // Tipos de projéteis disponíveis
    private static final String PROJECTILE_ARROW = "arrow";
    private static final String PROJECTILE_SLASH = "slash";
    private static final String PROJECTILE_FIREBALL = "fireball";
    private static final String PROJECTILE_ICE_BOLT = "ice_bolt";
    private static final String PROJECTILE_LIGHTNING = "lightning";
    private static final String PROJECTILE_MAGIC_BOLT = "magic_bolt";

    public ItemDataLoader() {
        this.weapons = new HashMap<>();
        this.armors = new HashMap<>();
        this.consumables = new HashMap<>();
        this.accessories = new HashMap<>();
        this.allItems = new HashMap<>();
    }

    public void loadAllItems() {
        logger.info("========================================");
        logger.info("        LOADING ITEM DEFINITIONS        ");
        logger.info("========================================");

        loadWeapons();
        loadArmors();
        loadConsumables();
        loadAccessories();

        validateItemAnimations();
        logger.info("========================================");
        logger.info("Total items loaded: {} (Weapons: {}, Armors: {}, Consumables: {}, Accessories: {})",
                allItems.size(), weapons.size(), armors.size(), consumables.size(), accessories.size());
        logger.info("========================================");
    }

    private void validateItemAnimations() {
        logger.info("Validating item animations...");

        for (ItemDefinition item : allItems.values()) {
            if (item.isWeapon() && item.getProjectileAnimationId() != null) {
                String animId = item.getProjectileAnimationId();
                AnimationType animType = AnimationType.fromId(animId);

                if (animType == null) {

                    String defaultAnim = item.isRanged() ?
                            AnimationType.getDefaultForRanged().getId() :
                            AnimationType.getDefaultForMelee().getId();
                    item.setProjectileAnimationId(defaultAnim);
                    logger.warn("Weapon '{}' não possui animação '{}' e foi usado a '{}' como fallback", item.getId(), animId, defaultAnim);

                } else {
                    // Verificar compatibilidade
                    if (item.isRanged() && !animType.isRanged()) {
                        String defaultAnim = AnimationType.getDefaultForRanged().getId();
                        item.setProjectileAnimationId(defaultAnim);
                        logger.warn("Ranged Weapon '{}' não possui animação '{}' e foi usado a '{}' como fallback", item.getId(), animId, defaultAnim);

                    } else if (!item.isRanged() && !item.isMagic() && !animType.isMelee()) {
                        logger.warn("Melee weapon '{}' uses non-melee animation '{}'", item.getId(), animId);
                        // Para melee, pode ser aceito, mas logamos warning
                        logger.info("  → Keeping '{}' but may cause visual issues", animId);
                    }
                }
            }
        }
    }

    private void loadWeapons() {
        logger.info("Loading weapons...");

        // ========== ARMAS CORPO A CORPO (MELEE) ==========
        // Formato: addMeleeWeapon(id, name, damage, cooldown, tileX, tileY, description, staminaCost, manaCost, projectileId)

        addMeleeWeapon("simple_sword", "Espada Simples", 10, 1.0f, 1, 0,
                "Uma espada básica de ferro forjada por aprendizes. Leve e equilibrada, ideal para iniciantes.",
                99, 0, PROJECTILE_SLASH);

        addMeleeWeapon("iron_sword", "Espada de Ferro", 18, 1.25f, 1, 0,
                "Espada forjada em ferro puro. Possui um bom equilíbrio entre peso e dano.",
                8, 0, PROJECTILE_SLASH);

        addMeleeWeapon("dagger", "Adaga", 6, 0.67f, 2, 0,
                "Uma adaga rápida e discreta. Perfeita para ataques velozes e precisos.",
                3, 0, PROJECTILE_SLASH);

        addMeleeWeapon("simple_axe", "Machado Simples", 14, 1.43f, 3, 0,
                "Machado de madeira com lâmina de ferro. Causa dano pesado, mas é mais lento.",
                12, 0, PROJECTILE_SLASH);

        addMeleeWeapon("great_sword", "Espadão", 25, 2.0f, 4, 0,
                "Uma enorme espada de duas mãos. Extremamente pesada, mas devastadora.",
                15, 0, PROJECTILE_SLASH);

        addMeleeWeapon("war_hammer", "Martelo de Guerra", 22, 1.8f, 5, 0,
                "Martelo pesado que esmaga armaduras. Alto dano, muito lento.",
                18, 0, PROJECTILE_SLASH);

        // ========== ARCOS (RANGED FÍSICO) ==========
        // Formato: addRangedWeapon(id, name, damage, cooldown, tileX, tileY, projectileSpeed, projectileRange, description, staminaCost, manaCost, projectileId)

        addRangedWeapon("simple_bow", "Arco Simples", 8, 1.25f, 0, 1, 600f, 400f,
                "Arco de madeira flexível. Ideal para caça e treinamento de precisão.",
                8, 0, PROJECTILE_ARROW);

        addRangedWeapon("long_bow", "Arco Longo", 15, 1.67f, 1, 1, 900f, 550f,
                "Arco longo élfico com alcance superior. Perfeito para ataques à longa distância.",
                12, 0, PROJECTILE_ARROW);

        addRangedWeapon("quick_bow", "Arco Rápido", 6, 0.77f, 2, 1, 450f, 300f,
                "Arco curto otimizado para disparos rápidos. Sacrifica dano por velocidade.",
                6, 0, PROJECTILE_ARROW);

        addRangedWeapon("crossbow", "Besta", 20, 2.5f, 3, 1, 800f, 500f,
                "Besta pesada de alto dano. Recarregamento lento, mas letal.",
                15, 0, PROJECTILE_ARROW);

        // ========== ARMAS MÁGICAS (RANGED MÁGICO) ==========
        // Formato: addMagicWeapon(id, name, damage, cooldown, tileX, tileY, projectileSpeed, projectileRange, description, manaCost, staminaCost, projectileId)

        addMagicWeapon("fire_wand", "Varinha de Fogo", 12, 1.2f, 3, 2, 400f, 800f,
                "Varinha que lança bolas de fogo. Causa dano em área.",
                49, 0, PROJECTILE_FIREBALL);

        addMagicWeapon("ice_staff", "Cajado de Gelo", 10, 1.5f, 1, 3, 650f, 500f,
                "Cajado que lança projéteis de gelo. Congela inimigos.",
                12, 0, PROJECTILE_ICE_BOLT);

        addMagicWeapon("lightning_rod", "Vara de Raios", 18, 2.0f, 2, 3, 800f, 600f,
                "Vara que invoca raios. Alto dano, alto custo de mana.",
                25, 0, PROJECTILE_LIGHTNING);

        addMagicWeapon("magic_staff", "Cajado Arcano", 14, 1.3f, 3, 3, 750f, 550f,
                "Cajado canalizador de energia arcana. Disparos mágicos puros.",
                18, 0, PROJECTILE_MAGIC_BOLT);

        addMagicWeapon("healing_staff", "Cajado de Cura", 5, 1.0f, 4, 3, 500f, 400f,
                "Cajado que lança energia curativa. Cura aliados.",
                10, 5, PROJECTILE_MAGIC_BOLT);

        addMagicWeapon("necromancer_wand", "Varinha Necromante", 16, 1.8f, 5, 3, 600f, 450f,
                "Varinha sombria que drena a vida dos inimigos.",
                20, 0, PROJECTILE_MAGIC_BOLT);

        logger.info("Loaded {} weapons", weapons.size());
    }

    /**
     * Adiciona uma arma corpo a corpo
     */
    private void addMeleeWeapon(String id, String name, int damage, float cooldown, int tileX, int tileY,
                                String description, int staminaCost, int manaCost, String projectileId) {
        ItemDefinition def = new ItemDefinition();
        def.setId(id);
        def.setName(name);
        def.setDescription(description);
        def.setRarity(Rarity.COMMON);
        def.setCategory("weapon");
        def.setSpritesheet("itens/spritesheet_itens.png");
        def.setTileX(tileX);
        def.setTileY(tileY);
        def.setDamage(damage);
        def.setAttackId("melee_" + id);
        def.setAttackAnimation("sword_slash");
        def.setAttackCooldown(cooldown);
        def.setRanged(false);
        def.setHitboxDuration(0.5f);

        // Definir custos
        def.setStaminaCost(staminaCost);
        def.setManaCost(manaCost);

        // Projétil para efeito visual (slash)
        def.setProjectileId(projectileId);
        def.setProjectileSpeed(3000f);
        def.setProjectileRange(100f);
        def.setProjectileAnimationId(projectileId);

        weapons.put(id, def);
        allItems.put(id, def);
    }

    /**
     * Adiciona uma arma de longo alcance física (arcos, bestas)
     */
    private void addRangedWeapon(String id, String name, int damage, float cooldown, int tileX, int tileY,
                                 float projectileSpeed, float projectileRange, String description,
                                 int staminaCost, int manaCost, String projectileId) {
        ItemDefinition def = new ItemDefinition();
        def.setId(id);
        def.setName(name);
        def.setDescription(description);
        def.setRarity(Rarity.COMMON);
        def.setCategory("weapon");
        def.setSpritesheet("itens/spritesheet_itens.png");
        def.setTileX(tileX);
        def.setTileY(tileY);
        def.setDamage(damage);
        def.setAttackId("ranged_" + id);
        def.setAttackAnimation("bow_shoot");
        def.setAttackCooldown(cooldown);
        def.setRanged(true);
        def.setHitboxDuration(0.5f);

        // Definir custos
        def.setStaminaCost(staminaCost);
        def.setManaCost(manaCost);

        // Configurações do projétil
        def.setProjectileId(projectileId);
        def.setProjectileSpeed(projectileSpeed);
        def.setProjectileRange(projectileRange);
        def.setProjectileAnimationId(projectileId);

        weapons.put(id, def);
        allItems.put(id, def);
    }

    /**
     * Adiciona uma arma mágica (custo de mana)
     */
    private void addMagicWeapon(String id, String name, int damage, float cooldown, int tileX, int tileY,
                                float projectileSpeed, float projectileRange, String description,
                                int manaCost, int staminaCost, String projectileId) {
        ItemDefinition def = new ItemDefinition();
        def.setId(id);
        def.setName(name);
        def.setDescription(description);
        def.setRarity(Rarity.UNCOMMON);
        def.setCategory("weapon");
        def.setSpritesheet("itens/spritesheet_itens.png");
        def.setTileX(tileX);
        def.setTileY(tileY);
        def.setDamage(damage);
        def.setAttackId("magic_" + id);
        def.setAttackAnimation("cast");
        def.setAttackCooldown(cooldown);
        def.setRanged(true);
        def.setHitboxDuration(0.5f);

        // Definir custos (mana prioritário)
        def.setStaminaCost(staminaCost);
        def.setManaCost(manaCost);

        // Configurações do projétil
        def.setProjectileId(projectileId);
        def.setProjectileSpeed(projectileSpeed);
        def.setProjectileRange(projectileRange);
        def.setProjectileAnimationId(projectileId);

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
        helmet.setArmorSlot("helmet");
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
        chest.setArmorSlot("chest");
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
        legs.setArmorSlot("legs");
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
        boots.setArmorSlot("boots");
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
        hood.setArmorSlot("helmet");
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
        robe.setArmorSlot("chest");
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
        pants.setArmorSlot("legs");
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
        boots.setArmorSlot("boots");
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

        ItemDefinition manaPotion = new ItemDefinition();
        manaPotion.setId("mana_potion");
        manaPotion.setName("Poção de Mana");
        manaPotion.setDescription("Poção azul que restaura 30 de Mana.");
        manaPotion.setCategory("consumable");
        manaPotion.setSpritesheet("itens/spritesheet_itens.png");
        manaPotion.setTileX(4);
        manaPotion.setTileY(1);
        manaPotion.setManaAmount(30);
        consumables.put(manaPotion.getId(), manaPotion);
        allItems.put(manaPotion.getId(), manaPotion);

        ItemDefinition staminaPotion = new ItemDefinition();
        staminaPotion.setId("stamina_potion");
        staminaPotion.setName("Poção de Stamina");
        staminaPotion.setDescription("Poção verde que restaura 40 de Stamina.");
        staminaPotion.setCategory("consumable");
        staminaPotion.setSpritesheet("itens/spritesheet_itens.png");
        staminaPotion.setTileX(5);
        staminaPotion.setTileY(1);
        staminaPotion.setStaminaAmount(40);
        consumables.put(staminaPotion.getId(), staminaPotion);
        allItems.put(staminaPotion.getId(), staminaPotion);

        logger.info("Loaded {} consumables", consumables.size());
    }

    private void loadAccessories() {
        logger.info("Loading accessories...");

        // Anel de Força
        ItemDefinition strengthRing = new ItemDefinition();
        strengthRing.setId("ring_of_strength");
        strengthRing.setName("Anel de Força");
        strengthRing.setDescription("Um anel de ferro que aumenta o poder físico.");
        strengthRing.setCategory("accessory");
        strengthRing.setAccessorySlot("ring");
        strengthRing.setSpritesheet("itens/spritesheet_itens.png");
        strengthRing.setTileX(0);
        strengthRing.setTileY(2);
        strengthRing.setBonusPhysicalPower(5);
        strengthRing.setBonusMaxHp(20);
        accessories.put(strengthRing.getId(), strengthRing);
        allItems.put(strengthRing.getId(), strengthRing);

        // Anel de Magia
        ItemDefinition magicRing = new ItemDefinition();
        magicRing.setId("ring_of_magic");
        magicRing.setName("Anel de Magia");
        magicRing.setDescription("Um anel com uma gema azul que amplifica poderes arcanos.");
        magicRing.setCategory("accessory");
        magicRing.setAccessorySlot("ring");
        magicRing.setSpritesheet("itens/spritesheet_itens.png");
        magicRing.setTileX(1);
        magicRing.setTileY(2);
        magicRing.setBonusMagicPower(5);
        magicRing.setBonusMaxMana(30);
        accessories.put(magicRing.getId(), magicRing);
        allItems.put(magicRing.getId(), magicRing);

        // Anel de Resistência
        ItemDefinition resistanceRing = new ItemDefinition();
        resistanceRing.setId("ring_of_resistance");
        resistanceRing.setName("Anel de Resistência");
        resistanceRing.setDescription("Anel que concede resistência elemental.");
        resistanceRing.setCategory("accessory");
        resistanceRing.setAccessorySlot("ring");
        resistanceRing.setSpritesheet("itens/spritesheet_itens.png");
        resistanceRing.setTileX(2);
        resistanceRing.setTileY(2);
        resistanceRing.setBonusFireResistance(10);
        resistanceRing.setBonusIceResistance(10);
        resistanceRing.setBonusLightningResistance(10);
        accessories.put(resistanceRing.getId(), resistanceRing);
        allItems.put(resistanceRing.getId(), resistanceRing);

        // Colar de Vida
        ItemDefinition lifeNecklace = new ItemDefinition();
        lifeNecklace.setId("necklace_of_life");
        lifeNecklace.setName("Colar de Vida");
        lifeNecklace.setDescription("Um colar que concede vitalidade extra.");
        lifeNecklace.setCategory("accessory");
        lifeNecklace.setAccessorySlot("necklace");
        lifeNecklace.setSpritesheet("itens/spritesheet_itens.png");
        lifeNecklace.setTileX(3);
        lifeNecklace.setTileY(2);
        lifeNecklace.setBonusMaxHp(50);
        lifeNecklace.setBonusHpRegen(2);
        accessories.put(lifeNecklace.getId(), lifeNecklace);
        allItems.put(lifeNecklace.getId(), lifeNecklace);

        // Capa das Sombras
        ItemDefinition shadowCloak = new ItemDefinition();
        shadowCloak.setId("cloak_of_shadows");
        shadowCloak.setName("Capa das Sombras");
        shadowCloak.setDescription("Uma capa escura que aumenta a agilidade e esquiva.");
        shadowCloak.setCategory("accessory");
        shadowCloak.setAccessorySlot("cloak");
        shadowCloak.setSpritesheet("itens/spritesheet_itens.png");
        shadowCloak.setTileX(4);
        shadowCloak.setTileY(2);
        shadowCloak.setBonusMovementSpeed(25);
        shadowCloak.setBonusDodgeChance(0.03f);
        shadowCloak.setBonusAttackSpeed(0.05f);
        accessories.put(shadowCloak.getId(), shadowCloak);
        allItems.put(shadowCloak.getId(), shadowCloak);

        // Amuleto de Velocidade (Trinket)
        ItemDefinition speedTrinket = new ItemDefinition();
        speedTrinket.setId("trinket_of_speed");
        speedTrinket.setName("Amuleto de Velocidade");
        speedTrinket.setDescription("Um amuleto que aumenta a velocidade de ataque.");
        speedTrinket.setCategory("accessory");
        speedTrinket.setAccessorySlot("trinket");
        speedTrinket.setSpritesheet("itens/spritesheet_itens.png");
        speedTrinket.setTileX(5);
        speedTrinket.setTileY(2);
        speedTrinket.setBonusAttackSpeed(0.10f);
        speedTrinket.setBonusMovementSpeed(15);
        accessories.put(speedTrinket.getId(), speedTrinket);
        allItems.put(speedTrinket.getId(), speedTrinket);

        // Cristal de Mana (Trinket)
        ItemDefinition manaTrinket = new ItemDefinition();
        manaTrinket.setId("crystal_of_mana");
        manaTrinket.setName("Cristal de Mana");
        manaTrinket.setDescription("Um cristal que aumenta a regeneração de mana.");
        manaTrinket.setCategory("accessory");
        manaTrinket.setAccessorySlot("trinket");
        manaTrinket.setSpritesheet("itens/spritesheet_itens.png");
        manaTrinket.setTileX(6);
        manaTrinket.setTileY(2);
        manaTrinket.setBonusMaxMana(50);
        manaTrinket.setBonusManaRegen(3);
        manaTrinket.setBonusCooldownReduction(0.05f);
        accessories.put(manaTrinket.getId(), manaTrinket);
        allItems.put(manaTrinket.getId(), manaTrinket);

        // Brinco de Precisão (Trinket)
        ItemDefinition precisionEarring = new ItemDefinition();
        precisionEarring.setId("earring_of_precision");
        precisionEarring.setName("Brinco de Precisão");
        precisionEarring.setDescription("Um brinco que aumenta a chance crítica.");
        precisionEarring.setCategory("accessory");
        precisionEarring.setAccessorySlot("trinket");
        precisionEarring.setSpritesheet("itens/spritesheet_itens.png");
        precisionEarring.setTileX(7);
        precisionEarring.setTileY(2);
        precisionEarring.setBonusCriticalChance(0.05f);
        precisionEarring.setBonusCriticalDamage(0.10f);
        accessories.put(precisionEarring.getId(), precisionEarring);
        allItems.put(precisionEarring.getId(), precisionEarring);

        // Amuleto de Mana (Trinket)
        ItemDefinition manaAmulet = new ItemDefinition();
        manaAmulet.setId("amulet_of_mana");
        manaAmulet.setName("Amuleto de Mana");
        manaAmulet.setDescription("Amuleto que reduz custo de mana de habilidades.");
        manaAmulet.setCategory("accessory");
        manaAmulet.setAccessorySlot("trinket");
        manaAmulet.setSpritesheet("itens/spritesheet_itens.png");
        manaAmulet.setTileX(8);
        manaAmulet.setTileY(2);
        manaAmulet.setBonusCooldownReduction(0.08f);
        manaAmulet.setBonusManaRegen(2);
        accessories.put(manaAmulet.getId(), manaAmulet);
        allItems.put(manaAmulet.getId(), manaAmulet);

        logger.info("Loaded {} accessories", accessories.size());
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

    public Map<String, ItemDefinition> getAccessories() {
        return new HashMap<>(accessories);
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