package net.repo.spellbook;

import net.repo.spellbook.item.SpellBookItem;
import net.repo.spellbook.util.PermissionsHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpellBookMod implements ModInitializer {
    public static final String MOD_ID = "spellbookmod";
    
    // Fixed: Create the explicit Registry Key first so Mojang knows what the item is named
    public static final ResourceKey<Item> SPELLBOOK_KEY = ResourceKey.create(
        BuiltInRegistries.ITEM.key(), 
        Identifier.fromNamespaceAndPath(MOD_ID, "spellbook")
    );

    // Fixed: Instantiate the item passing the explicitly registered Key directly into properties
    public static final Item SPELLBOOK_ITEM = new SpellBookItem(
        new Item.Properties().setId(SPELLBOOK_KEY).stacksTo(1)
    );

    public static final Map<UUID, BlockPos> GATEWAY_POS = new HashMap<>();
    public static final Map<UUID, ResourceKey<Level>> GATEWAY_WORLD = new HashMap<>();

    @Override
    public void onInitialize() {
        // Registers the item cleanly using the pre-defined key
        Registry.register(BuiltInRegistries.ITEM, SPELLBOOK_KEY, SPELLBOOK_ITEM);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(SPELLBOOK_ITEM);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("givespellbook")
                .requires(PermissionsHelper.require(2))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "target");
                        player.getInventory().add(new ItemStack(SPELLBOOK_ITEM));
                        context.getSource().sendSuccess(() -> Component.literal("Gave Spellbook to " + player.getName().getString()), true);
                        return 1;
                    })
                )
            );
        });

        ServerTickEvents.END_LEVEL_TICK.register(server -> {
            for (ServerPlayer player : server.getServer().getPlayerList().getPlayers()) {
                ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty()) {
                    CustomData customData = mainHand.get(DataComponents.CUSTOM_DATA);
                    if (customData != null && customData.copyTag().contains("SpellbookTool")) {
                        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 20, 4, false, false, true));
                    }
                }
            }
        });
    }
}