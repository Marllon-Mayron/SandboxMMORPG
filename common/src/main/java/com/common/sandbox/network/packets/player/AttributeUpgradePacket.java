package com.common.sandbox.network.packets.player;

import com.common.sandbox.network.Packet;
import java.util.Map;

public class AttributeUpgradePacket extends Packet {
    public Map<String, Integer> upgrades;

    public AttributeUpgradePacket() {}

    public AttributeUpgradePacket(Map<String, Integer> upgrades) {
        this.upgrades = upgrades;
    }
}