package dev.shadowsoffire.hostilenetworks;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;

import dev.shadowsoffire.hostilenetworks.client.DataModelTooltipRenderer;
import dev.shadowsoffire.hostilenetworks.client.DeepLearnerHudRenderer;
import dev.shadowsoffire.hostilenetworks.client.Offset;
import dev.shadowsoffire.hostilenetworks.client.Offset.AnchorPoint;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerScreen;
import dev.shadowsoffire.hostilenetworks.gui.LootFabScreen;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberScreen;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.net.OpenDeepLearnerPayload;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.config.Property;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class HostileClient {

    public static final KeyMapping KEY_OPEN_DEEP_LEARNER = new KeyMapping("key.hostilenetworks.open_deep_learner", GLFW.GLFW_KEY_U, "key.categories.hostilenetworks");

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

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(KEY_OPEN_DEEP_LEARNER);
    }

    @EventBusSubscriber(bus = Bus.GAME, value = Dist.CLIENT, modid = HostileNetworks.MODID)
    public static class GameBusEvents {

        private static final SuggestionProvider<CommandSourceStack> SUGGEST_ANCHOR_POINT = (ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(AnchorPoint.values()).map(Enum::name).map(s -> s.toLowerCase(Locale.ROOT)),
            builder);

        @SubscribeEvent
        public static void commands(RegisterClientCommandsEvent e) {
            e.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("hnn_client")
                    .then(LiteralArgumentBuilder.<CommandSourceStack>literal("set_hud_pos")
                        .then(Commands.argument("anchor", StringArgumentType.string()).suggests(SUGGEST_ANCHOR_POINT)
                            .executes(c -> {
                                updateHudPos(AnchorPoint.parse(c.getArgument("anchor", String.class)), 0, 0);
                                return 0;
                            })
                            .then(Commands.argument("x", IntegerArgumentType.integer(-1000, 1000))
                                .then(Commands.argument("y", IntegerArgumentType.integer(-1000, 1000))
                                    .executes(c -> {
                                        updateHudPos(AnchorPoint.parse(c.getArgument("anchor", String.class)), c.getArgument("x", Integer.class), c.getArgument("y", Integer.class));
                                        return 0;
                                    }))))));
        }

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

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post e) {
            if (KEY_OPEN_DEEP_LEARNER.consumeClick() && Minecraft.getInstance().screen == null) {
                PacketDistributor.sendToServer(OpenDeepLearnerPayload.INSTANCE);
            }
        }

        private static void updateHudPos(AnchorPoint anchor, int x, int y) {
            Configuration cfg = HostileNetworks.cfg;
            HostileConfig.deepLearnerOffset = new Offset(anchor, x, y);

            Property anchorProp = cfg.get("client", "Deep Learner HUD Anchor Point", "");
            anchorProp.setValue(anchor.name().toLowerCase(Locale.ROOT));

            Property xProp = cfg.get("client", "Deep Learner HUD X Offset", 0);
            xProp.setValue(x);

            Property yProp = cfg.get("client", "Deep Learner HUD Y Offset", 0);
            yProp.setValue(y);

            cfg.save();
        }

    }

}
