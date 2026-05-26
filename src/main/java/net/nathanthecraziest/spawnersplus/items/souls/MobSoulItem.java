package net.nathanthecraziest.spawnersplus.items.souls;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public class MobSoulItem extends ModSoulItem {

    @Override
    public Text getName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        EntityType<?> type = getEntityType(nbt);
        if (type == null) return super.getName(stack);
        return Text.translatable(this.getTranslationKey(), type.getName());
    }

    public static ItemStack stackFor(MobSoulItem item, EntityType<?> type) {
        ItemStack stack = new ItemStack(item);
        NbtCompound root = new NbtCompound();
        NbtCompound entityTag = new NbtCompound();
        entityTag.putString("id", EntityType.getId(type).toString());
        root.put("EntityTag", entityTag);
        stack.setNbt(root);
        return stack;
    }

    public MobSoulItem(FabricItemSettings settings) {
        super(settings, EntityType.PIG);
    }
}
