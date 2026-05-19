package com.common.sandbox.network.packets.inventory;

import com.common.sandbox.model.combat.AttackDefinition;
import com.common.sandbox.model.item.ItemDefinition;
import com.common.sandbox.network.Packet;
import java.util.Map;

public class ItemDefinitionSyncPacket extends Packet {
    public Map<String, ItemDefinition> itemDefinitions;
    public Map<String, AttackDefinition> attackDefinitions;

    public ItemDefinitionSyncPacket() {}

    public ItemDefinitionSyncPacket(Map<String, ItemDefinition> definitions) {
        this.itemDefinitions = definitions;
        this.attackDefinitions = null;
    }

    // NOVO CONSTRUTOR
    public ItemDefinitionSyncPacket(Map<String, ItemDefinition> definitions,
                                    Map<String, AttackDefinition> attackDefs) {
        this.itemDefinitions = definitions;
        this.attackDefinitions = attackDefs;
    }
}