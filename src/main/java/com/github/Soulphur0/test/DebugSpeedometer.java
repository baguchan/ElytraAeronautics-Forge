package com.github.Soulphur0.test;


import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DebugSpeedometer {

    static Vec3 lastPos = Vec3.ZERO;

    static public void displayDebugSpeedometer(Vec3 pos, Level world) {
        // Calculate speed.
        String message = "SPEED = " + (
                Math.round(
                        Math.sqrt(
                                Math.pow(pos.x - lastPos.x, 2) +
                                        Math.pow(pos.y - lastPos.y, 2) +
                                        Math.pow(pos.z - lastPos.z, 2))
                                * 20 /* 20 ticks every second */ * 100.0)) / 100.0 + "m/s";
        lastPos = pos;
        // Send speed info.
        if (world.isClientSide()) {
            if (world.getServer() != null) {
                List<? extends Player> players = world.getServer().getPlayerList().getPlayers();
                players.forEach(player -> player.displayClientMessage(Component.translatable(message), true));
            }
        }
    }

    // Para usar speedometer escribir esta línea en el método modifyVelocity de LivingEntityMixin
    // ! DEBUG SPEEDOMETER
    // DebugSpeedometer.displayDebugSpeedometer(super.getPos(), this.getWorld());
    // ! DEBUG SPEEDOMETER
}
