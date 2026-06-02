package net.nathanthecraziest.spawnersplus.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.nathanthecraziest.spawnersplus.config.MobSoulDropConfig;
import net.nathanthecraziest.spawnersplus.items.ModItemGroup;
import net.nathanthecraziest.spawnersplus.items.ModItems;
import net.nathanthecraziest.spawnersplus.items.souls.MobSoulItem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SpawnersPlusCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("spawnersplus")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(giveNode())
                        .then(literal("droprate")
                                .then(globalNode())
                                .then(modNode())
                                .then(entityNode()))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> giveNode() {
        return literal("give")
                .then(argument("targets", EntityArgumentType.players())
                        .then(argument("entity", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(mobTypeIds(context.getSource().getServer()), builder))
                                .executes(context -> giveSoul(context.getSource(),
                                        EntityArgumentType.getPlayers(context, "targets"),
                                        IdentifierArgumentType.getIdentifier(context, "entity")))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> globalNode() {
        return literal("global")
                .then(literal("get").executes(context -> getGlobal(context.getSource())))
                .then(literal("set")
                        .then(argument("rate", FloatArgumentType.floatArg(0f, 1f))
                                .executes(context -> setGlobal(context.getSource(), FloatArgumentType.getFloat(context, "rate")))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> modNode() {
        return literal("mod")
                .then(argument("modid", StringArgumentType.word())
                        .then(literal("get")
                                .executes(context -> getMod(context.getSource(), StringArgumentType.getString(context, "modid"))))
                        .then(literal("set")
                                .then(argument("rate", FloatArgumentType.floatArg(0f, 1f))
                                        .executes(context -> setMod(context.getSource(),
                                                StringArgumentType.getString(context, "modid"),
                                                FloatArgumentType.getFloat(context, "rate")))))
                        .then(literal("remove")
                                .executes(context -> removeMod(context.getSource(), StringArgumentType.getString(context, "modid")))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> entityNode() {
        return literal("entity")
                .then(argument("entity", IdentifierArgumentType.identifier())
                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(droprateEntityIds(context.getSource().getServer()), builder))
                        .then(literal("get")
                                .executes(context -> getEntity(context.getSource(),
                                        IdentifierArgumentType.getIdentifier(context, "entity"))))
                        .then(literal("set")
                                .then(argument("rate", FloatArgumentType.floatArg(0f, 1f))
                                        .executes(context -> setEntity(context.getSource(),
                                                IdentifierArgumentType.getIdentifier(context, "entity"),
                                                FloatArgumentType.getFloat(context, "rate")))))
                        .then(literal("remove")
                                .executes(context -> removeEntity(context.getSource(),
                                        IdentifierArgumentType.getIdentifier(context, "entity")))));
    }

    // Probed the same way as the creative tab populator, cached because suggestions run per keystroke
    private static volatile Set<Identifier> mobTypeIds;

    private static Set<Identifier> mobTypeIds(MinecraftServer server) {
        Set<Identifier> ids = mobTypeIds;
        if (ids == null) {
            ids = new HashSet<>();
            for (EntityType<?> type : Registries.ENTITY_TYPE) {
                try {
                    Entity probe = type.create(server.getOverworld());
                    if (probe != null) {
                        if (probe instanceof MobEntity) ids.add(EntityType.getId(type));
                        probe.discard();
                    }
                } catch (Throwable ignored) {}
            }
            mobTypeIds = ids;
        }
        return ids;
    }

    private static Set<Identifier> droprateEntityIds(MinecraftServer server) {
        Set<Identifier> ids = new HashSet<>(mobTypeIds(server));
        for (EntityType<?> type : ModItemGroup.LEGACY_SOUL_TYPES) {
            ids.remove(EntityType.getId(type));
        }
        return ids;
    }

    private static boolean isLegacy(Identifier entityId) {
        EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
        return type != null && ModItemGroup.LEGACY_SOUL_TYPES.contains(type);
    }

    private static int giveSoul(ServerCommandSource source, Collection<ServerPlayerEntity> targets, Identifier entityId) {
        EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
        if (type == null || !mobTypeIds(source.getServer()).contains(entityId)) {
            source.sendError(Text.literal(entityId + " is not a spawnable mob"));
            return 0;
        }

        MobSoulItem soul = (MobSoulItem) ModItems.MOB_SOUL;
        Text name = MobSoulItem.stackFor(soul, type).getName();
        for (ServerPlayerEntity player : targets) {
            player.getInventory().offerOrDrop(MobSoulItem.stackFor(soul, type));
        }

        if (targets.size() == 1) {
            String playerName = targets.iterator().next().getName().getString();
            source.sendFeedback(() -> Text.literal("Gave 1 ").append(name).append(" to " + playerName), true);
        } else {
            source.sendFeedback(() -> Text.literal("Gave 1 ").append(name).append(" to " + targets.size() + " players"), true);
        }
        return targets.size();
    }

    private static int getGlobal(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Global mob soul drop rate: " + MobSoulDropConfig.getGlobalDefault()), false);
        return 1;
    }

    private static int setGlobal(ServerCommandSource source, float rate) {
        MobSoulDropConfig.setGlobalDefault(rate);
        MobSoulDropConfig.save();
        source.sendFeedback(() -> Text.literal("Global mob soul drop rate set to " + rate), true);
        return 1;
    }

    private static int getMod(ServerCommandSource source, String modid) {
        if (MobSoulDropConfig.hasModDefault(modid)) {
            float rate = MobSoulDropConfig.snapshotModDefaults().get(modid);
            source.sendFeedback(() -> Text.literal(modid + " mob soul drop rate: " + rate), false);
        } else {
            float fallback = MobSoulDropConfig.getGlobalDefault();
            source.sendFeedback(() -> Text.literal(modid + " has no mod default, falls back to global " + fallback), false);
        }
        return 1;
    }

    private static int setMod(ServerCommandSource source, String modid, float rate) {
        MobSoulDropConfig.setModDefault(modid, rate);
        MobSoulDropConfig.save();
        source.sendFeedback(() -> Text.literal(modid + " mob soul drop rate set to " + rate), true);
        return 1;
    }

    private static int removeMod(ServerCommandSource source, String modid) {
        if (!MobSoulDropConfig.removeModDefault(modid)) {
            source.sendError(Text.literal(modid + " has no mod default to remove"));
            return 0;
        }

        MobSoulDropConfig.save();
        source.sendFeedback(() -> Text.literal("Removed mod default for " + modid), true);
        return 1;
    }

    private static int getEntity(ServerCommandSource source, Identifier entityId) {
        if (isLegacy(entityId)) {
            source.sendFeedback(() -> Text.literal(entityId + " is a legacy soul, its rate lives in soul_drop_rates.json"), false);
            return 1;
        }

        float rate = MobSoulDropConfig.rateFor(entityId);
        String from;
        if (MobSoulDropConfig.hasOverride(entityId)) {
            from = "entity override";
        } else if (MobSoulDropConfig.hasModDefault(entityId.getNamespace())) {
            from = "mod default";
        } else {
            from = "global default";
        }
        source.sendFeedback(() -> Text.literal(entityId + " mob soul drop rate: " + rate + " (" + from + ")"), false);
        return 1;
    }

    private static int setEntity(ServerCommandSource source, Identifier entityId, float rate) {
        if (!mobTypeIds(source.getServer()).contains(entityId)) {
            source.sendError(Text.literal(entityId + " is not a spawnable mob"));
            return 0;
        }
        if (isLegacy(entityId)) {
            source.sendError(Text.literal(entityId + " is a legacy soul, its rate lives in soul_drop_rates.json"));
            return 0;
        }

        MobSoulDropConfig.setOverride(entityId, rate);
        MobSoulDropConfig.save();
        source.sendFeedback(() -> Text.literal(entityId + " mob soul drop rate set to " + rate), true);
        return 1;
    }

    private static int removeEntity(ServerCommandSource source, Identifier entityId) {
        if (!MobSoulDropConfig.removeOverride(entityId)) {
            source.sendError(Text.literal(entityId + " has no override to remove"));
            return 0;
        }

        MobSoulDropConfig.save();
        source.sendFeedback(() -> Text.literal("Removed override for " + entityId), true);
        return 1;
    }
}
