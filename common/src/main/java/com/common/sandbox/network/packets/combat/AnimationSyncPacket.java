package com.common.sandbox.network.packets.combat;

import com.common.sandbox.model.combat.ProjectileAnimation;
import com.common.sandbox.network.Packet;
import java.util.Map;

public class AnimationSyncPacket extends Packet {
    public Map<String, ProjectileAnimation> projectileAnimations;

    public AnimationSyncPacket() {}

    public AnimationSyncPacket(Map<String, ProjectileAnimation> animations) {
        this.projectileAnimations = animations;
    }
}