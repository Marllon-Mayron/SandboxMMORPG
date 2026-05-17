package com.sandbox.server;

import com.common.sandbox.model.enums.ArmorSet;
import com.common.sandbox.model.item.ItemDefinition;
import com.common.sandbox.model.item.SetBonus;
import com.common.sandbox.model.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Gerencia os bônus de conjuntos de armadura (4 peças)
 */
public class SetManager {
    private static final Logger logger = LoggerFactory.getLogger(SetManager.class);
    private static SetManager instance;

    private final Map<String, Set<ArmorSet>> activeSets;

    private SetManager() {
        this.activeSets = new HashMap<>();
    }

    public static synchronized SetManager getInstance() {
        if (instance == null) {
            instance = new SetManager();
        }
        return instance;
    }

    /**
     * Verifica se o jogador está usando um conjunto completo
     */
    public boolean isUsingFullSet(Player player, ArmorSet set) {
        if (player == null || set == null || player.getInventory() == null) return false;

        Map<String, String> equipped = player.getInventory().getEquipped();

        String[] requiredSlots = {"helmet", "chest", "legs", "boots"};
        int piecesFound = 0;

        for (String slot : requiredSlots) {
            String itemId = equipped.get(slot);
            if (itemId != null && !itemId.isEmpty()) {
                ItemDefinition def = ItemManager.getInstance().getItemDefinition(itemId);
                if (def != null && set.equals(def.getArmorSet())) {
                    piecesFound++;
                }
            }
        }

        return piecesFound >= 4;
    }

    /**
     * Aplica todos os bônus de conjunto para um jogador
     */
    public void applySetBonuses(Player player) {
        if (player == null) return;

        removeSetBonuses(player);

        Set<ArmorSet> activeSetList = new HashSet<>();

        for (ArmorSet set : ArmorSet.values()) {
            if (isUsingFullSet(player, set)) {
                SetBonus bonus = set.getFullSetBonus();
                if (bonus != null) {
                    bonus.applyToPlayer(player);
                    activeSetList.add(set);
                    logger.info("Applied full set bonus for {} to player {}",
                            set.getDisplayName(), player.getUsername());
                }
            }
        }

        if (!activeSetList.isEmpty()) {
            activeSets.put(player.getId(), activeSetList);
        }

        player.validateCurrentStats();
    }

    /**
     * Remove todos os bônus de conjunto de um jogador
     */
    public void removeSetBonuses(Player player) {
        if (player == null) return;

        Set<ArmorSet> oldSets = activeSets.remove(player.getId());
        if (oldSets != null) {
            for (ArmorSet set : oldSets) {
                SetBonus bonus = set.getFullSetBonus();
                if (bonus != null) {
                    bonus.removeFromPlayer(player);
                }
            }
            logger.info("Removed set bonuses for player {}", player.getUsername());
        }
    }

    /**
     * Recalcula os bônus após troca de equipamento
     */
    public void refreshSetBonuses(Player player) {
        if (player == null) return;
        removeSetBonuses(player);
        applySetBonuses(player);
    }
}