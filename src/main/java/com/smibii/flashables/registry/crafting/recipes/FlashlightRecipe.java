package com.smibii.flashables.registry.crafting.recipes;

import com.smibii.flashables.registry.FlashablesItems;
import com.smibii.flashables.registry.crafting.Crafting;
import com.smibii.flashables.registry.crafting.RecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FlashlightRecipe extends CustomRecipe {
    public FlashlightRecipe(ResourceLocation id, CraftingBookCategory cat) {
        super(id, cat);
    }

    @Override
    public boolean matches(@NotNull CraftingContainer inv, @NotNull Level level) {
        if (inv.getWidth() < 3 || inv.getHeight() < 3) return false;

        // Pattern (strict, no mirroring):
        // row 0: i i i
        // row 1: g l b
        // row 2: i i i
        // And nothing else anywhere.

        for (int y = 0; y < inv.getHeight(); y++) {
            for (int x = 0; x < inv.getWidth(); x++) {
                ItemStack s = inv.getItem(y * inv.getWidth() + x);

                if (x < 3 && y < 3) {
                    if (!checkSlot(x, y, s)) return false;
                } else {
                    if (!s.isEmpty()) return false;
                }
            }
        }
        return true;
    }

    private boolean checkSlot(int x, int y, ItemStack s) {
        return switch (y) {
            case 0, 2 -> s.is(Items.IRON_INGOT);
            case 1 -> switch (x) {
                case 0 -> Crafting.PANE_TO_COLOR.containsKey(s.getItem());
                case 1 -> s.is(Items.REDSTONE_LAMP);
                case 2 -> s.is(ItemTags.BUTTONS);
                default -> false;
            };
            default -> false;
        };
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer inv, @NotNull RegistryAccess access) {
        int color = 0;

        ItemStack paneStack = inv.getItem(inv.getWidth());
        Integer mapped = Crafting.PANE_TO_COLOR.get(paneStack.getItem());
        if (mapped != null) color = mapped;

        ItemStack out = new ItemStack(FlashablesItems.FLASHLIGHT.get());
        out.getOrCreateTag().putInt("Color", color);
        out.getOrCreateTag().putInt("Power", 0);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w >= 3 && h >= 3;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.FLASHLIGHT.get();
    }
}
