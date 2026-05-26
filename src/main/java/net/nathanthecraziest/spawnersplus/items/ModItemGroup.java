package net.nathanthecraziest.spawnersplus.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.nathanthecraziest.spawnersplus.SpawnersPlus;
import net.nathanthecraziest.spawnersplus.blocks.ModBlocks;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ModItemGroup {

    public static final Set<EntityType<?>> LEGACY_SOUL_TYPES = new HashSet<>(Set.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER,
            EntityType.BLAZE, EntityType.MAGMA_CUBE, EntityType.SILVERFISH, EntityType.STRAY,
            EntityType.WITHER_SKELETON, EntityType.HUSK, EntityType.DROWNED, EntityType.CREEPER
    ));

    public static Consumer<ItemGroup.Entries> clientDynamicPopulator = null;

    private static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.SPAWNER_FRAGMENT))
            .displayName(Text.translatable("itemGroup.spawnersplus.itemGroupName"))
            .entries((context, entries) -> {
                entries.add(ModBlocks.INACTIVE_SPAWNER);
                entries.add(ModItems.SPAWNER_FRAGMENT);
                entries.add(ModItems.SPAWNER_SILENCER);
                entries.add(ModItems.ZOMBIE_SOUL);
                entries.add(ModItems.SKELETON_SOUL);
                entries.add(ModItems.SPIDER_SOUL);
                entries.add(ModItems.CAVE_SPIDER_SOUL);
                entries.add(ModItems.BLAZE_SOUL);
                entries.add(ModItems.MAGMA_CUBE_SOUL);
                entries.add(ModItems.STRAY_SOUL);
                entries.add(ModItems.WITHER_SKELETON_SOUL);
                entries.add(ModItems.HUSK_SOUL);
                entries.add(ModItems.CREEPER_SOUL);

                Consumer<ItemGroup.Entries> populator = clientDynamicPopulator;
                if (populator != null) populator.accept(entries);

                entries.add(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(ModEnchantments.SOUL_STEALING, 1)));
            })
            .build();


    public static void registerModItemGroups(){
        SpawnersPlus.LOGGER.debug("Registering Item Groups for " + SpawnersPlus.MOD_ID);

        Registry.register(Registries.ITEM_GROUP, new Identifier(SpawnersPlus.MOD_ID, "spawnersplus"), ITEM_GROUP);
    }
}
