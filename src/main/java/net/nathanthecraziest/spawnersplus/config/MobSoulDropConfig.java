package net.nathanthecraziest.spawnersplus.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;
import net.nathanthecraziest.spawnersplus.SpawnersPlus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobSoulDropConfig {

    private static final String CONFIG_PATH = "config/spawnersplus/mob_soul_drop_rates.json";
    private static final String DEFAULT_KEY = "default";
    private static final float DEFAULT_RATE = 0.04f;

    private static final Map<String, Float> OVERRIDES = new HashMap<>();
    private static final Map<String, Float> MOD_DEFAULTS = new HashMap<>();
    private static volatile float defaultRate = DEFAULT_RATE;

    public static float rateFor(Identifier entityId) {
        Float specific = OVERRIDES.get(entityId.toString());
        if (specific != null) return clamp(specific);
        Float modDefault = MOD_DEFAULTS.get(entityId.getNamespace());
        if (modDefault != null) return clamp(modDefault);
        return clamp(defaultRate);
    }

    public static float getGlobalDefault() { return defaultRate; }

    public static Map<String, Float> snapshotOverrides() { return new HashMap<>(OVERRIDES); }

    public static Map<String, Float> snapshotModDefaults() { return new HashMap<>(MOD_DEFAULTS); }

    public static boolean hasOverride(Identifier entityId) {
        return OVERRIDES.containsKey(entityId.toString());
    }

    public static boolean hasModDefault(String namespace) {
        return MOD_DEFAULTS.containsKey(namespace);
    }

    public static void setGlobalDefault(float v) { defaultRate = clamp(v); }

    public static void setOverride(Identifier entityId, float v) {
        OVERRIDES.put(entityId.toString(), clamp(v));
    }

    public static void setModDefault(String namespace, float v) {
        MOD_DEFAULTS.put(namespace, clamp(v));
    }

    public static boolean removeOverride(Identifier entityId) {
        return OVERRIDES.remove(entityId.toString()) != null;
    }

    public static boolean removeModDefault(String namespace) {
        return MOD_DEFAULTS.remove(namespace) != null;
    }

    public static void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"default\": ").append(defaultRate);

        List<String> mods = new ArrayList<>(MOD_DEFAULTS.keySet());
        Collections.sort(mods);
        for (String mod : mods) {
            sb.append(",\n  \"").append(mod).append(":default\": ").append(MOD_DEFAULTS.get(mod));
        }

        List<String> entities = new ArrayList<>(OVERRIDES.keySet());
        Collections.sort(entities);
        for (String key : entities) {
            sb.append(",\n  \"").append(key).append("\": ").append(OVERRIDES.get(key));
        }

        sb.append("\n}\n");
        Config.createFile(CONFIG_PATH, sb.toString(), true);
    }

    public static void load() {
        File file = Config.createFile(CONFIG_PATH, defaultJson(), false);
        JsonObject json = Config.getJsonObject(Config.readFile(file));
        if (json == null) return;
        OVERRIDES.clear();
        MOD_DEFAULTS.clear();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            float val;
            try {
                val = entry.getValue().getAsFloat();
            } catch (Exception e) {
                SpawnersPlus.LOGGER.warn("mob_soul_drop_rates.json: invalid value for '{}', skipping", key);
                continue;
            }

            if (DEFAULT_KEY.equals(key)) {
                defaultRate = val;
                continue;
            }

            Identifier id;
            try {
                id = new Identifier(key);
            } catch (Exception e) {
                SpawnersPlus.LOGGER.warn("mob_soul_drop_rates.json: malformed key '{}' (expected namespace:path or 'default'), skipping", key);
                continue;
            }

            if (DEFAULT_KEY.equals(id.getPath())) {
                MOD_DEFAULTS.put(id.getNamespace(), val);
            } else {
                OVERRIDES.put(id.toString(), val);
            }
        }
    }

    private static String defaultJson() {
        return "{\n" +
               "  \"default\": 0.04,\n" +
               "  \"minecraft:ender_dragon\": 0.5,\n" +
               "  \"minecraft:warden\": 0.33,\n" +
               "  \"minecraft:wither\": 0.25\n" +
               "}";
    }

    private static float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
