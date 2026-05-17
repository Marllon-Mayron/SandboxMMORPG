package com.common.sandbox.model.enums;

import com.common.sandbox.model.item.SetBonus;
import java.io.Serializable;

/**
 * Conjuntos de armadura disponíveis no jogo
 * O bônus é ativado apenas quando o jogador usa todas as 4 peças
 */
public enum ArmorSet implements Serializable {

    // Conjunto do Camponês - Bônus para Guerreiro/Tank
    PEASANT("peasant", "Conjunto do Camponês",
            SetBonus.warrior(50, 15, 10, 20f, 0.05f)),

    // Conjunto do Aprendiz de Mago - Bônus para Mago
    MAGE_APPRENTICE("mage_apprentice", "Conjunto do Aprendiz de Mago",
            SetBonus.mage(100, 15, 15, 0.10f, 0.08f));

    private final String id;
    private final String displayName;
    private final SetBonus fullSetBonus;

    ArmorSet(String id, String displayName, SetBonus fullSetBonus) {
        this.id = id;
        this.displayName = displayName;
        this.fullSetBonus = fullSetBonus;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public SetBonus getFullSetBonus() { return fullSetBonus; }

    public static ArmorSet fromId(String id) {
        for (ArmorSet set : values()) {
            if (set.id.equals(id)) {
                return set;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}