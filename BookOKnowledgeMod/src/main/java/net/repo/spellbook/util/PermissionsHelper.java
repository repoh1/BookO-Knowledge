package net.repo.spellbook.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import java.util.function.Predicate;

public class PermissionsHelper {
    /**
     * Checks permission profiles natively matching the modern level array mapping.
     * Evaluates true if the player meets or exceeds the required permission level.
     * Works seamlessly on Dedicated Multiplayer Servers and Singleplayer worlds.
     */
    public static Predicate<CommandSourceStack> require(int fallbackOpLevel) {
        // Safe bound array index selector to retrieve Mojang's native PermissionLevel enum
        final PermissionLevel requiredLevel;
        if (fallbackOpLevel >= 0 && fallbackOpLevel < PermissionLevel.values().length) {
            requiredLevel = PermissionLevel.values()[fallbackOpLevel];
        } else {
            requiredLevel = PermissionLevel.values()[2]; // Fallback to Level 2 (OP) if out of bounds
        }

        return source -> {
            try {
                ServerPlayer player = source.getPlayerOrException();
                
                // Grabs the player's level set from the server and evaluates it against our target level enum
                return source.getServer()
                             .getProfilePermissions(player.nameAndId())
                             .level()
                             .isEqualOrHigherThan(requiredLevel);
            } catch (Exception e) {
                // Automatically return true for non-players (Server Console / RCON / Command Blocks)
                return true;
            }
        };
    }
}