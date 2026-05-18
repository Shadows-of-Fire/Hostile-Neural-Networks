package dev.shadowsoffire.hostilenetworks;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import dev.shadowsoffire.hostilenetworks.Hostile.Items;
import dev.shadowsoffire.hostilenetworks.HostileConfig.ConfigPayload;
import dev.shadowsoffire.hostilenetworks.command.GenerateModelCommand;
import dev.shadowsoffire.hostilenetworks.command.GiveModelCommand;
import dev.shadowsoffire.hostilenetworks.curios.CuriosCompat;
import dev.shadowsoffire.hostilenetworks.data.BlockDataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.EntityDataModel;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HostileNetworks.MODID)
public class HostileEvents {

    @SubscribeEvent
    public static void cmds(RegisterCommandsEvent e) {
        var builder = Commands.literal("hostilenetworks");
        GenerateModelCommand.register(builder);
        GiveModelCommand.register(builder);
        e.getDispatcher().register(builder);
    }

    @SubscribeEvent
    public static void modelAttunement(EntityInteractSpecific e) {
        if (!HostileConfig.rightClickToAttune) return;
        Player player = e.getEntity();
        ItemStack stack = player.getItemInHand(e.getHand());
        if (stack.is(Hostile.Items.BLANK_DATA_MODEL)) {
            if (!player.level().isClientSide) {
                Collection<EntityDataModel> models = DataModelRegistry.INSTANCE.getForEntity(e.getTarget().getType());
                EntityDataModel match = pickMatch(player, models, m -> m.attunesTo((ServerPlayer) player, e.getTarget()));
                if (match == null) return;
                completeAttunement(player, e.getHand(), match);
            }
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent
    public static void modelAttunementBlock(RightClickBlock e) {
        if (!HostileConfig.rightClickToAttune) return;
        Player player = e.getEntity();
        ItemStack stack = player.getItemInHand(e.getHand());
        if (stack.is(Hostile.Items.BLANK_DATA_MODEL)) {
            if (player.level() instanceof ServerLevel level) {
                BlockPos pos = e.getPos();
                BlockState state = level.getBlockState(pos);
                Collection<BlockDataModel> models = DataModelRegistry.INSTANCE.getForBlock(state.getBlock());
                BlockDataModel match = pickMatch(player, models, m -> m.attunesTo(level, pos));
                if (match == null) return;
                completeAttunement(player, e.getHand(), match);
            }
            // Cancel so blocks with their own right-click behavior (chests, repeaters) don't fire while a blank model is in hand.
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @Nullable
    private static <M extends DataModel> M pickMatch(Player player, Collection<M> candidates, Predicate<M> matcher) {
        if (candidates == null || candidates.isEmpty()) {
            player.sendSystemMessage(Component.translatable("hostilenetworks.msg.no_model").withStyle(ChatFormatting.RED));
            return null;
        }
        M match = null;
        for (M model : candidates) {
            if (matcher.test(model)) {
                if (match != null) {
                    Component msg = Component.translatable("hostilenetworks.msg.multiple_models", model.name(), match.name()).withStyle(ChatFormatting.RED);
                    player.sendSystemMessage(msg);
                    return null;
                }
                match = model;
            }
        }
        if (match == null) {
            player.sendSystemMessage(Component.translatable("hostilenetworks.msg.no_model").withStyle(ChatFormatting.RED));
        }
        return match;
    }

    private static void completeAttunement(Player player, InteractionHand hand, DataModel match) {
        Component msg = Component.translatable("hostilenetworks.msg.built", match.name()).withStyle(ChatFormatting.GOLD);
        player.sendSystemMessage(msg);

        ItemStack modelStack = new ItemStack(Hostile.Items.DATA_MODEL);
        DataModelItem.setStoredModel(modelStack, match);
        player.setItemInHand(hand, modelStack);
    }

    @SubscribeEvent
    public static void kill(LivingDeathEvent e) {
        if (!HostileConfig.actionUpgradesModel) return;
        LivingEntity killed = e.getEntity();
        if (killed.getKillCredit() instanceof ServerPlayer p) {
            forEachLearner(p, dl -> updateModels(dl, killed.getType(), 0));
            if (ModList.get().isLoaded("curios")) {
                CuriosCompat.tryUpdateDeepLearner(p, killed.getType(), 0);
            }
        }
    }

    @SubscribeEvent
    public static void mine(BlockEvent.BreakEvent e) {
        if (!HostileConfig.actionUpgradesModel) return;
        if (e.getPlayer() instanceof ServerPlayer p) {
            Block block = e.getState().getBlock();
            forEachLearner(p, dl -> updateModels(dl, block, 0));
            if (ModList.get().isLoaded("curios")) {
                CuriosCompat.tryUpdateDeepLearner(p, block, 0);
            }
        }
    }

    private static void forEachLearner(ServerPlayer p, Consumer<ItemStack> action) {
        p.getInventory().items.stream().filter(s -> s.is(Items.DEEP_LEARNER)).forEach(action);
        if (p.getOffhandItem().is(Items.DEEP_LEARNER)) {
            action.accept(p.getOffhandItem());
        }
    }

    public static void updateModels(ItemStack learner, EntityType<?> type, int bonus) {
        updateModels(learner, dm -> dm instanceof EntityDataModel e
            && (e.entity() == type || e.variants().contains(type)), bonus);
    }

    public static void updateModels(ItemStack learner, Block block, int bonus) {
        updateModels(learner, dm -> dm instanceof BlockDataModel b
            && (b.block().block() == block || b.variants().stream().anyMatch(v -> v.block() == block)), bonus);
    }

    private static void updateModels(ItemStack learner, Predicate<DataModel> matcher, int bonus) {
        ComponentItemHandler handler = DeepLearnerItem.getItemHandler(learner);
        for (int i = 0; i < 4; i++) {
            ItemStack model = handler.getStackInSlot(i);
            if (model.isEmpty()) continue;
            DynamicHolder<DataModel> dModel = DataModelItem.getStoredModel(model);
            if (dModel.isBound() && matcher.test(dModel.get())) {
                int data = DataModelItem.getData(model);
                ModelTier tier = ModelTierRegistry.getByData(dModel.get(), data);
                DataModelItem.setData(model, data + dModel.get().getDataGained(tier) + bonus);
                handler.setStackInSlot(i, model);
            }
        }
    }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent e) {
        e.addListener((ResourceManagerReloadListener) resman -> HostileNetworks.cfg = HostileConfig.load());
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent e) {
        ConfigPayload msg = new ConfigPayload();
        e.getRelevantPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, msg));
    }
}
