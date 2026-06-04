package net.nathanthecraziest.spawnersplus.client;

import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.nathanthecraziest.spawnersplus.SpawnersPlus;
import net.nathanthecraziest.spawnersplus.items.souls.MobSoulItem;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// Discovers hand-drawn soul textures at resource load and swaps them in for the tinted generic.
// Lookup per entity: item/<namespace>/<path>_soul.png first, then flat item/<path>_soul.png.
public class SoulTextureOverrides {

    private static final ModelIdentifier MOB_SOUL_MODEL = new ModelIdentifier(SpawnersPlus.MOD_ID, "mob_soul", "inventory");

    private static volatile Map<Identifier, Identifier> entityToModel = Map.of();

    public static boolean hasOverride(Identifier entityId) {
        return entityToModel.containsKey(entityId);
    }

    @Nullable
    public static BakedModel modelFor(Identifier entityId) {
        Identifier modelId = entityToModel.get(entityId);
        if (modelId == null) return null;
        return MinecraftClient.getInstance().getBakedModelManager().getModel(modelId);
    }

    public static void register() {
        PreparableModelLoadingPlugin.register(SoulTextureOverrides::discover, (discovered, pluginContext) -> {
            entityToModel = discovered.entityToModel;

            pluginContext.addModels(discovered.modelToTexture.keySet());
            pluginContext.resolveModel().register(context -> {
                Identifier texture = discovered.modelToTexture.get(context.id());
                if (texture == null) return null;
                return JsonUnbakedModel.deserialize(
                        "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + texture + "\"}}");
            });

            pluginContext.modifyModelAfterBake().register((model, context) -> {
                if (model != null && MOB_SOUL_MODEL.equals(context.id())) {
                    return new OverridingModel(model);
                }
                return model;
            });
        });
    }

    private static CompletableFuture<Discovered> discover(ResourceManager resourceManager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Identifier, Identifier> entityToModel = new HashMap<>();
            Map<Identifier, Identifier> modelToTexture = new HashMap<>();

            for (EntityType<?> type : Registries.ENTITY_TYPE) {
                Identifier id = EntityType.getId(type);
                Identifier texture = resolveTexture(resourceManager, id);
                if (texture == null) continue;

                Identifier model = new Identifier(SpawnersPlus.MOD_ID, "soul_override/" + id.getNamespace() + "/" + id.getPath());
                entityToModel.put(id, model);
                modelToTexture.put(model, texture);
            }

            return new Discovered(entityToModel, modelToTexture);
        }, executor);
    }

    @Nullable
    private static Identifier resolveTexture(ResourceManager resourceManager, Identifier entityId) {
        String namespaced = "item/" + entityId.getNamespace() + "/" + entityId.getPath() + "_soul";
        if (resourceManager.getResource(new Identifier(SpawnersPlus.MOD_ID, "textures/" + namespaced + ".png")).isPresent()) {
            return new Identifier(SpawnersPlus.MOD_ID, namespaced);
        }

        String flat = "item/" + entityId.getPath() + "_soul";
        if (resourceManager.getResource(new Identifier(SpawnersPlus.MOD_ID, "textures/" + flat + ".png")).isPresent()) {
            return new Identifier(SpawnersPlus.MOD_ID, flat);
        }

        return null;
    }

    private static class Discovered {
        final Map<Identifier, Identifier> entityToModel;
        final Map<Identifier, Identifier> modelToTexture;

        Discovered(Map<Identifier, Identifier> entityToModel, Map<Identifier, Identifier> modelToTexture) {
            this.entityToModel = entityToModel;
            this.modelToTexture = modelToTexture;
        }
    }

    private static class SoulOverrideList extends ModelOverrideList {

        @Override
        public BakedModel apply(BakedModel model, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed) {
            if (stack.getItem() instanceof MobSoulItem soul) {
                EntityType<?> type = soul.getEntityType(stack.getNbt());
                if (type != null) {
                    BakedModel override = modelFor(EntityType.getId(type));
                    if (override != null) return override;
                }
            }
            return model;
        }

        SoulOverrideList() {
            super(null, null, List.of()); // with no overrides the baker and parent go unused
        }
    }

    private static class OverridingModel extends ForwardingBakedModel {

        private final ModelOverrideList overrides = new SoulOverrideList();

        @Override
        public ModelOverrideList getOverrides() {
            return overrides;
        }

        OverridingModel(BakedModel wrapped) {
            this.wrapped = wrapped;
        }
    }
}
