package net.nathanthecraziest.spawnersplus.client;

import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.nathanthecraziest.spawnersplus.items.souls.MobSoulItem;

public class MobSoulColorProvider implements ItemColorProvider {

    private static final int FALLBACK_GRAY = 0xFF808080;

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return -1;
        if (!(stack.getItem() instanceof MobSoulItem soul)) return -1;
        NbtCompound nbt = stack.getNbt();
        EntityType<?> type = soul.getEntityType(nbt);
        if (type == null) return FALLBACK_GRAY;
        SpawnEggItem egg = SpawnEggItem.forEntity(type);
        if (egg == null) return FALLBACK_GRAY;
        return 0xFF000000 | egg.getColor(0);
    }
}
