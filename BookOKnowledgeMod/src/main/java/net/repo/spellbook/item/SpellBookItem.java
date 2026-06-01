package net.repo.spellbook.item;

import net.repo.spellbook.SpellBookMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.server.network.Filterable;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class SpellBookItem extends Item {
    private final Random random = new Random();

    public SpellBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!world.isClientSide() && user instanceof ServerPlayer player) {
            String action = getActionFromComponents(stack);

            if (action == null || action.isEmpty()) {
                ItemStack writtenBook = createInteractiveBook();
                player.getInventory().add(writtenBook);
                player.sendSystemMessage(Component.literal("§6[Spellbook] A tome appears in your inventory. Open it to cast spells.§r"));
                return InteractionResult.SUCCESS;
            } else {
                handlePowerExecution(player, action);
                stack.remove(DataComponents.CUSTOM_DATA);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    private String getActionFromComponents(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            // Fixed: Safely unwrap the Optional<String> or default to empty string
            Optional<String> optionalVal = nbt.getString("TriggerAction");
            if (optionalVal.isPresent() && !optionalVal.get().isEmpty()) {
                return optionalVal.get();
            }
        }
        return null;
    }

    private void handlePowerExecution(ServerPlayer player, String action) {
        ServerLevel world = (ServerLevel) player.level();

        switch (action) {
            case "spawn_piglin_brute" -> spawnMob(player, EntityType.PIGLIN_BRUTE, 5);
            case "spawn_creeper"      -> spawnMob(player, EntityType.CREEPER, 10);
            case "spawn_elder_guardian" -> spawnMob(player, EntityType.ELDER_GUARDIAN, 20);
            case "spawn_enderman"     -> spawnMob(player, EntityType.ENDERMAN, 15);
            case "spawn_wither"       -> spawnMob(player, EntityType.WITHER, 40);
            case "spawn_warden"       -> spawnMob(player, EntityType.WARDEN, 50);

            case "item_fractures" -> giveTool(player, "netherite_pickaxe", "Fractures", 25);
            case "item_lumber"    -> giveTool(player, "netherite_axe", "Lumber", 25);
            case "item_burrow"    -> giveTool(player, "netherite_shovel", "Burrow", 25);

            case "siphon" -> {
                if (deductXP(player, 50)) {
                    player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                    player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 2));
                    player.sendSystemMessage(Component.literal("§a❤ Siphon Activated! Fully Healed.§r"));
                }
            }

            case "randomizer" -> {
                if (deductXP(player, 50)) {
                    MobEffectInstance[] effects = {
                        new MobEffectInstance(MobEffects.STRENGTH, 400, 2),
                        new MobEffectInstance(MobEffects.SPEED, 400, 2),
                        new MobEffectInstance(MobEffects.REGENERATION, 400, 2),
                        new MobEffectInstance(MobEffects.INVISIBILITY, 400, 2),
                        new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 2)
                    };
                    player.addEffect(effects[random.nextInt(effects.length)]);
                    player.sendSystemMessage(Component.literal("§d✨ Random Level 3 effect applied!§r"));
                }
            }

            case "gateway" -> {
                UUID uuid = player.getUUID();
                if (!SpellBookMod.GATEWAY_POS.containsKey(uuid)) {
                    SpellBookMod.GATEWAY_POS.put(uuid, player.blockPosition());
                    SpellBookMod.GATEWAY_WORLD.put(uuid, world.dimension());
                    player.sendSystemMessage(Component.literal("§b📍 Gateway point established! Use again to teleport (Costs 250 Levels).§r"));
                } else {
                    if (deductXP(player, 250)) {
                        BlockPos targetPos = SpellBookMod.GATEWAY_POS.remove(uuid);
                        var targetWorldKey = SpellBookMod.GATEWAY_WORLD.remove(uuid);
                        ServerLevel targetWorld = world.getServer().getLevel(targetWorldKey);
                        if (targetWorld != null) {
                            player.teleportTo(targetWorld,
                                targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                                Set.of(), player.getYRot(), player.getXRot(), true);
                            world.playSound(null, player.blockPosition(),
                                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                        }
                    }
                }
            }

            case "rupture" -> {
                if (deductXP(player, 500)) {
                    Vec3 pos = player.position();
                    world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 5, 1.0, 1.0, 1.0, 0.1);
                    world.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.5f);
                    AABB box = new AABB(pos.subtract(25, 25, 25), pos.add(25, 25, 25));
                    for (Entity entity : world.getEntities(player, box)) {
                        if (entity instanceof LivingEntity living
                            && !(entity instanceof Player p && p.isCreative())) {
                            living.hurt(world.damageSources().magic(), Float.MAX_VALUE);
                        }
                    }
                    player.sendSystemMessage(Component.literal("§4💥 Kynara Rupture unleashed absolute destruction!§r"));
                }
            }
        }
    }

    private boolean deductXP(ServerPlayer player, int levels) {
        if (player.isCreative()) return true;
        if (player.experienceLevel >= levels) {
            player.giveExperienceLevels(-levels);
            return true;
        }
        player.sendSystemMessage(Component.literal("§cYou don't have enough XP levels! Required: " + levels + "§r"));
        return false;
    }

    private void spawnMob(ServerPlayer player, EntityType<?> type, int lvlCost) {
        if (deductXP(player, lvlCost)) {
            ServerLevel world = (ServerLevel) player.level();
            Entity entity = type.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (entity != null) {
                entity.setPos(player.getX(), player.getY(), player.getZ() + 2);
                world.addFreshEntity(entity);
            }
        }
    }

    private void giveTool(ServerPlayer player, String itemNamespace, String name, int lvlCost) {
        if (deductXP(player, lvlCost)) {
            String command = String.format(
                "give %s %s[enchantments={levels:{\"minecraft:efficiency\":5}},custom_data={SpellbookTool:true},custom_name='\"§b%s§r\"']",
                player.getName().getString(), itemNamespace, name
            );
            player.level().getServer().getCommands()
                .performPrefixedCommand(player.createCommandSourceStack(), command);
        }
    }

    private ItemStack createInteractiveBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        var page1 = Component.literal("[ Page 1: Summons ]\n\n")
            .append(createLink("• Piglin Brute (5Lvl)\n", "spawn_piglin_brute"))
            .append(createLink("• Creeper (10Lvl)\n", "spawn_creeper"))
            .append(createLink("• Enderman (15Lvl)\n", "spawn_enderman"))
            .append(createLink("• Elder Guardian (20Lvl)\n", "spawn_elder_guardian"))
            .append(createLink("• Wither (40Lvl)\n", "spawn_wither"))
            .append(createLink("• Warden (50Lvl)\n", "spawn_warden"));

        var page2 = Component.literal("[ Page 2: Artifacts ]\n\nTools grant Efficiency V. (25Lvls)\n\n")
            .append(createLink("[Fractured Pickaxe]\n\n", "item_fractures"))
            .append(createLink("[Lumber Axe]\n\n", "item_lumber"))
            .append(createLink("[Burrow Shovel]", "item_burrow"));

        var page3 = Component.literal("[ Page 3: Siphon ]\n\nInstantly heals you and grants Resistance III.\n\n")
            .append(createLink("ACTIVATE (50 Lvls)", "siphon"));

        var page4 = Component.literal("[ Page 4: Roulette ]\n\nBestows a random Level 3 status effect.\n\n")
            .append(createLink("ROLL EFFECT (50 Lvls)", "randomizer"));

        var page5 = Component.literal("[ Page 5: Gateway ]\n\nClick 1: Lock location.\nClick 2: Rift jump back.\n\n")
            .append(createLink("ENGAGE RIFT (250 Lvls)", "gateway"));

        var page6 = Component.literal("[ Page 6: Rupture ]\n\nTriggers a cataclysmic shockwave.\n\n")
            .append(createLink("UNLEASH RUPTURE (500 Lvls)", "rupture"));

        List<Filterable<Component>> filteredPages = List.of(
            Filterable.passThrough(page1),
            Filterable.passThrough(page2),
            Filterable.passThrough(page3),
            Filterable.passThrough(page4),
            Filterable.passThrough(page5),
            Filterable.passThrough(page6)
        );

        WrittenBookContent content = new WrittenBookContent(
            Filterable.passThrough("Spellbook"), "Ancient Magic", 0, filteredPages, true
        );

        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    private Component createLink(String text, String action) {
        String cmd = "/item modify entity @s weapon.mainhand set custom_data={TriggerAction:\"" + action + "\"}";
        return Component.literal(text)
            .withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand(cmd))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to consume levels and cast!"))));
    }
}