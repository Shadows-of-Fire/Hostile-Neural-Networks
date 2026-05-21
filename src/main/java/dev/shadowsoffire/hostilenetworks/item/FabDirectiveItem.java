package dev.shadowsoffire.hostilenetworks.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.hostilenetworks.util.FabSelection;
import dev.shadowsoffire.hostilenetworks.util.FabSelection.ProductionMode;
import dev.shadowsoffire.hostilenetworks.util.SavedSelections;
import dev.shadowsoffire.placebo.PlaceboClient;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.util.SpecialTooltipItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * An item which holds a set of Loot Fabricator targets.
 * <p>
 * It can copy the targets from a fabricator and apply them to another. It can also be used to open a GUI to configure targets on-the-fly.
 */
public class FabDirectiveItem extends Item implements SpecialTooltipItem {

    public FabDirectiveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (be instanceof LootFabTileEntity lootFab) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (player.isSecondaryUseActive()) {
                SavedSelections selections = ctx.getItemInHand().getOrDefault(Hostile.Components.FAB_SELECTIONS, SavedSelections.EMPTY);
                if (!selections.isEmpty()) {
                    lootFab.setSelections(selections.getSelections());
                    lootFab.setRedstoneState(selections.getRedstoneState());
                    player.sendSystemMessage(HostileNetworks.lang("text", "selections_applied", lootFab.getSelections().size(), ctx.getItemInHand().getDisplayName()).withColor(Color.LIME));
                    return InteractionResult.SUCCESS;
                }
            }
            else {
                SavedSelections selections = new SavedSelections(lootFab.getSelections(), lootFab.getRedstoneState());
                ctx.getItemInHand().set(Hostile.Components.FAB_SELECTIONS, selections);
                player.sendSystemMessage(HostileNetworks.lang("text", "selections_copied", lootFab.getSelections().size(), lootFab.getBlockState().getBlock().getName()).withColor(Color.LIME));
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
        }

        return super.useOn(ctx);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        // TODO: Implement FabDirectiveMenu to allow configuring on-the-fly.
        return super.use(level, player, usedHand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        list.add(Component.translatable(this.getDescriptionId() + ".desc2").withStyle(ChatFormatting.GRAY));
        if (FMLEnvironment.dist.isClient()) {
            ClientAccess.appendHoverText(stack, context, list, flag);
        }
    }

    public static class ClientAccess {

        public static void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
            SavedSelections selections = stack.getOrDefault(Hostile.Components.FAB_SELECTIONS, SavedSelections.EMPTY);
            if (selections.isEmpty()) {
                return;
            }

            list.add(CommonComponents.SPACE);

            Map<DynamicHolder<DataModel>, FabSelection> fabSelections = selections.getSelections();
            int selIdx = PlaceboClient.getTooltipScrollIndex(selections.size());

            Map.Entry<DynamicHolder<DataModel>, FabSelection> entry = new ArrayList<>(fabSelections.entrySet()).get(selIdx);
            DynamicHolder<DataModel> holder = entry.getKey();
            FabSelection sel = entry.getValue();
            int index = sel.current();
            DataModel model = holder.isBound() ? holder.get() : null;
            ItemStack drop = model != null && index >= 0 && index < model.fabDrops().size() ? model.fabDrops().get(index) : ItemStack.EMPTY;

            MutableComponent comp = HostileNetworks.lang("text", "stored_selection", selIdx + 1, selections.size()).withColor(Color.LIME);
            if (flag.hasShiftDown()) {
                comp.append(CommonComponents.SPACE);
                comp.append(HostileNetworks.lang("text", "scroll").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false)));
            }
            else {
                comp.append(CommonComponents.SPACE);
                comp.append(HostileNetworks.lang("text", "shift").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false)));
            }

            list.add(comp);

            if (model != null) {
                if (!drop.isEmpty()) {
                    Component input = model.getPredictionDrop().getHoverName();
                    Component output = drop.getHoverName();
                    int queued = sel.mode() == ProductionMode.QUEUE ? sel.entries().size() - 1 : 0;
                    if (queued > 0) {
                        comp = HostileNetworks.lang("text", "selection.queue", input, output, queued).withColor(Color.LIME);
                    }
                    else {
                        comp = HostileNetworks.lang("text", "selection", input, drop.getCount(), output).withColor(Color.LIME);
                    }
                    list.add(comp);
                }
                else {
                    comp = HostileNetworks.lang("text", "invalid_selection.index", index).withStyle(ChatFormatting.RED);
                    list.add(comp);
                }
            }
            else {
                comp = HostileNetworks.lang("text", "invalid_selection.model", holder.getId().toString()).withStyle(ChatFormatting.RED);
                list.add(comp);
            }

        }
    }

}
