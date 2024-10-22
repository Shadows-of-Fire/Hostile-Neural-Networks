package dev.shadowsoffire.hostilenetworks;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;

import dev.shadowsoffire.hostilenetworks.client.DataModelTooltipRenderer;
import dev.shadowsoffire.hostilenetworks.client.DeepLearnerHudRenderer;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerScreen;
import dev.shadowsoffire.hostilenetworks.gui.LootFabScreen;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberScreen;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.items.ComponentItemHandler;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class HostileClient {

    @SubscribeEvent
    public static void mrl(ModelEvent.RegisterAdditional e) {
        e.register(ModelResourceLocation.standalone(HostileNetworks.loc("item/data_model_base")));
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> {
            DynamicHolder<DataModel> model = DataModelItem.getStoredModel(stack);
            int color = 0xFFFFFF;
            if (model.isBound()) {
                color = model.get().getNameColor();
            }
            return 0xFF000000 | color;
        }, Hostile.Items.PREDICTION.value());
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiLayersEvent e) {
        e.registerAboveAll(HostileNetworks.loc("deep_learner"), new DeepLearnerHudRenderer());
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent e) {
        e.register(Hostile.Containers.DEEP_LEARNER, DeepLearnerScreen::new);
        e.register(Hostile.Containers.SIM_CHAMBER, SimChamberScreen::new);
        e.register(Hostile.Containers.LOOT_FABRICATOR, LootFabScreen::new);
    }

    @SubscribeEvent
    public static void tooltipComps(RegisterClientTooltipComponentFactoriesEvent e) {
        e.register(DataModelInstance.class, DataModelTooltipRenderer::new);
    }

    @EventBusSubscriber(bus = Bus.GAME, value = Dist.CLIENT, modid = HostileNetworks.MODID)
    public static class GameBusEvents {

        @SubscribeEvent
        public static void comps(RenderTooltipEvent.GatherComponents e) {
            ItemStack stack = e.getItemStack();

            if (stack.is(Hostile.Items.DEEP_LEARNER)) {
                List<Either<FormattedText, TooltipComponent>> list = e.getTooltipElements();
                int rmvIdx = -1;
                for (int i = 0; i < list.size(); i++) {
                    Optional<FormattedText> o = list.get(i).left();
                    if (o.isPresent() && o.get() instanceof Component comp && comp.getContents() instanceof LiteralContents tc) {
                        if ("DL_INV_MARKER".equals(tc.text())) {
                            rmvIdx = i;
                            list.remove(i);
                            break;
                        }
                    }
                }

                if (rmvIdx != -1) {
                    ComponentItemHandler inv = DeepLearnerItem.getItemHandler(stack);
                    for (int i = 3; i >= 0; i--) {
                        DataModelInstance model = new DataModelInstance(inv.getStackInSlot(i), i);
                        if (model.isValid()) {
                            e.getTooltipElements().add(rmvIdx, Either.right(model));
                        }

                    }
                }
            }
        }

    }

}
