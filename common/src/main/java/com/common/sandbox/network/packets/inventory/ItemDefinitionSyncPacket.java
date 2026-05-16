package com.common.sandbox.network.packets.inventory;

import com.common.sandbox.model.item.ItemDefinition;
import com.common.sandbox.network.Packet;
import java.util.Map;

public class ItemDefinitionSyncPacket extends Packet {
    public Map<String, ItemDefinition> itemDefinitions;

    public ItemDefinitionSyncPacket() {}

    public ItemDefinitionSyncPacket(Map<String, ItemDefinition> definitions) {
        this.itemDefinitions = definitions;
    }
}