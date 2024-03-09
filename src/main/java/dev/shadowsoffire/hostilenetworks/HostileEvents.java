package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.hostilenetworks.Hostile.Items;
import dev.shadowsoffire.hostilenetworks.HostileConfig.ConfigMessage;
import dev.shadowsoffire.hostilenetworks.command.GenerateModelCommand;
import dev.shadowsoffire.hostilenetworks.command.GiveModelCommand;
import dev.shadowsoffire.hostilenetworks.curios.CuriosCompat;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.placebo.network.PacketDistro;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.items.ItemStackHandler;

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
        if (stack.getItem() == Hostile.Items.BLANK_DATA_MODEL.get()) {
            if (!player.level().isClientSide) {
                DataModel model = DataModelRegistry.INSTANCE.getForEntity(e.getTarget().getType());
                if (model == null) {
                    Component msg = Component.translatable("hostilenetworks.msg.no_model").withStyle(ChatFormatting.RED);
                    player.sendSystemMessage(msg);
                    return;
                }

                Component msg = Component.translatable("hostilenetworks.msg.built", model.name()).withStyle(ChatFormatting.GOLD);
                player.sendSystemMessage(msg);

                ItemStack modelStack = new ItemStack(Hostile.Items.DATA_MODEL.get());
                DataModelItem.setStoredModel(modelStack, model);
                player.setItemInHand(e.getHand(), modelStack);
            }
            e.setResult(Result.DENY);
        }
    }

    @SubscribeEvent
    public static void kill(LivingDeathEvent e) {
        if (!HostileConfig.killModelUpgrade) return;
        Entity src = e.getSource().getEntity();
        if (src instanceof ServerPlayer p) {
            p.getInventory().items.stream().filter(s -> s.getItem() == Items.DEEP_LEARNER.get()).forEach(dl -> updateModels(dl, e.getEntity().getType(), 0));
            if (p.getOffhandItem().getItem() == Items.DEEP_LEARNER.get()) updateModels(p.getOffhandItem(), e.getEntity().getType(), 0);
            if (ModList.get().isLoaded("curios")) {
                ItemStack curioStack = CuriosCompat.getDeepLearner(p);
                if (curioStack.getItem() == Items.DEEP_LEARNER.get()) updateModels(curioStack, e.getEntity().getType(), 0);
            }
        }
    }

    public static void updateModels(ItemStack learner, EntityType<?> type, int bonus) {
        ItemStackHandler handler = DeepLearnerItem.getItemHandler(learner);
        for (int i = 0; i < 4; i++) {
            ItemStack model = handler.getStackInSlot(i);
            if (model.isEmpty()) continue;
            DynamicHolder<DataModel> dModel = DataModelItem.getStoredModel(model);
            if (dModel.isBound() && dModel.get().type() == type || dModel.get().subtypes().contains(type)) {
                int data = DataModelItem.getData(model);
                ModelTier tier = ModelTier.getByData(dModel, data);
                DataModelItem.setData(model, data + dModel.get().getDataPerKill(tier) + bonus);
            }
        }
        DeepLearnerItem.saveItems(learner, handler);
    }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent e) {
        e.addListener((ResourceManagerReloadListener) resman -> HostileConfig.load());
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent e) {
        if (e.getPlayer() != null) {
            PacketDistro.sendTo(HostileNetworks.CHANNEL, new ConfigMessage(), e.getPlayer());
        }
        else {
            PacketDistro.sendToAll(HostileNetworks.CHANNEL, new ConfigMessage());
        }
    }
}
