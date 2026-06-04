package net.nathanthecraziest.spawnersplus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import net.nathanthecraziest.spawnersplus.blocks.ModBlocks;
import net.nathanthecraziest.spawnersplus.client.MobSoulColorProvider;
import net.nathanthecraziest.spawnersplus.client.SoulTextureOverrides;
import net.nathanthecraziest.spawnersplus.items.ModItemGroup;
import net.nathanthecraziest.spawnersplus.items.ModItems;
import net.nathanthecraziest.spawnersplus.items.souls.MobSoulItem;

public class SpawnersPlusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INACTIVE_SPAWNER, RenderLayer.getCutoutMipped());

        ColorProviderRegistry.ITEM.register(new MobSoulColorProvider(), ModItems.MOB_SOUL);
        SoulTextureOverrides.register();

        ModItemGroup.clientDynamicPopulator = entries -> {
            MobSoulItem mobSoul = (MobSoulItem) ModItems.MOB_SOUL;
            World probeWorld = MinecraftClient.getInstance().world;
            if (probeWorld == null) return;
            for (EntityType<?> type : Registries.ENTITY_TYPE) {
                if (ModItemGroup.LEGACY_SOUL_TYPES.contains(type)) continue;
                try {
                    Entity probe = type.create(probeWorld);
                    if (!(probe instanceof MobEntity)) {
                        if (probe != null) probe.discard();
                        continue;
                    }
                    probe.discard();
                    entries.add(MobSoulItem.stackFor(mobSoul, type));
                } catch (Throwable ignored) {}
            }
        };
    }
}
