package net.nathanthecraziest.spawnersplus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.nathanthecraziest.spawnersplus.blocks.ModBlocks;
import net.nathanthecraziest.spawnersplus.client.MobSoulColorProvider;
import net.nathanthecraziest.spawnersplus.items.ModItems;

public class SpawnersPlusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INACTIVE_SPAWNER, RenderLayer.getCutoutMipped());

        ColorProviderRegistry.ITEM.register(new MobSoulColorProvider(), ModItems.MOB_SOUL);
    }
}
