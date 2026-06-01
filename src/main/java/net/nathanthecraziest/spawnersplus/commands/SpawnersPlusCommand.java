package net.nathanthecraziest.spawnersplus.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.nathanthecraziest.spawnersplus.config.MobSoulDropConfig;
import net.nathanthecraziest.spawnersplus.items.ModItems;
import net.nathanthecraziest.spawnersplus.items.souls.MobSoulItem;

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
                .then(argument("entity", IdentifierArgumentType.identifier())
                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder))
                        .executes(context -> giveSoul(context.getSource(), IdentifierArgumentType.getIdentifier(context, "entity"))));
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
                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder))
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

    private static int giveSoul(ServerCommandSource source, Identifier entityId) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
        if (type == null) {
            source.sendError(Text.literal("Unknown entity type " + entityId));
            return 0;
        }

        ItemStack stack = MobSoulItem.stackFor((MobSoulItem) ModItems.MOB_SOUL, type);
        player.getInventory().offerOrDrop(stack);
        source.sendFeedback(() -> Text.literal("Gave 1 ").append(stack.getName()), true);
        return 1;
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
