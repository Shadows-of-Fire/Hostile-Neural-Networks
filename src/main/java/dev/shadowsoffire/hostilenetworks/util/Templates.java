package dev.shadowsoffire.hostilenetworks.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Helpers for {@link ItemStackTemplate}.
 */
public class Templates {

    /**
     * Creates the stack described by a template without the strict validation of {@link ItemStackTemplate#create()}.
     * <p>
     * Several data models define drops larger than the item's max stack size (e.g. 32 snowballs). 1.21 produced those as
     * oversized stacks and split them on insertion; {@code ItemStackTemplate#create} would instead log a warning and
     * return an empty stack, silently dropping the output.
     */
    public static ItemStack create(ItemStackTemplate template) {
        ItemStack stack = new ItemStack(template.item(), 1, template.components());
        stack.setCount(template.count());
        return stack;
    }

}
