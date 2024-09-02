package dev.shadowsoffire.hostilenetworks.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Give command for Data Models.
 * <p>
 * Syntax: /hostilenetworks give_model targets data_model model_tier bonus_data
 * <p>
 * targets is an entity selector for players.<br>
 * data_model is the registry name of a data model.<br>
 * model_tier is the tier of the model to give.<br>
 * bonus_data is extra model data added on top of the tier. This field is optional.
 */
public class GiveModelCommand {

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_MODEL_TIER = (ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(ModelTier.values()).map(t -> t.name().toLowerCase(Locale.ROOT)), builder);

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(
            Commands.literal("give_model")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("model", ResourceLocationArgument.id()).suggests(GenerateModelCommand.SUGGEST_DATA_MODEL)
                        .then(Commands.argument("tier", StringArgumentType.word()).suggests(SUGGEST_MODEL_TIER)
                            .executes(c -> {
                                return giveItem(c.getSource(), ResourceLocationArgument.getId(c, "model"), EntityArgument.getPlayers(c, "targets"), StringArgumentType.getString(c, "tier"), 0);
                            })
                            .then(Commands.argument("data", IntegerArgumentType.integer(0))
                                .executes(c -> {
                                    return giveItem(c.getSource(), ResourceLocationArgument.getId(c, "model"), EntityArgument.getPlayers(c, "targets"), StringArgumentType.getString(c, "tier"),
                                        IntegerArgumentType.getInteger(c, "data"));
                                }))))));
    }

    private static int giveItem(CommandSourceStack pSource, ResourceLocation modelId, Collection<ServerPlayer> pTargets, String tierName, int bonusData) throws CommandSyntaxException {
        DataModel model = DataModelRegistry.INSTANCE.getValue(modelId);
        if (model == null) {
            pSource.sendFailure(Component.literal("Invalid model: " + modelId));
            return 0;
        }
        else {
            int data;
            try {
                ModelTier tier = ModelTier.valueOf(tierName.toUpperCase(Locale.ROOT));
                data = tier.data().requiredData();
            }
            catch (Exception ex) {
                pSource.sendFailure(Component.literal("Invalid model tier: " + tierName));
                return -1;
            }

            data += bonusData;

            ItemStack modelStack = new ItemStack(Hostile.Items.DATA_MODEL);
            DataModelItem.setStoredModel(modelStack, model);
            DataModelItem.setData(modelStack, data);

            for (ServerPlayer serverplayer : pTargets) {
                ItemStack stack = modelStack.copy();
                boolean flag = serverplayer.getInventory().add(stack);
                if (flag && stack.isEmpty()) {
                    stack.setCount(1);
                    ItemEntity itementity1 = serverplayer.drop(stack, false);
                    if (itementity1 != null) {
                        itementity1.makeFakeItem();
                    }

                    serverplayer.level().playSound((Player) null, serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                        ((serverplayer.getRandom().nextFloat() - serverplayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    serverplayer.containerMenu.broadcastChanges();
                }
                else {
                    ItemEntity itementity = serverplayer.drop(stack, false);
                    if (itementity != null) {
                        itementity.setNoPickUpDelay();
                        itementity.setTarget(serverplayer.getUUID());
                    }
                }
            }

            if (pTargets.size() == 1) {
                pSource.sendSuccess(() -> {
                    return Component.translatable("commands.give.success.single", 1, modelStack.getDisplayName(), pTargets.iterator().next().getDisplayName());
                }, true);
            }
            else {
                pSource.sendSuccess(() -> {
                    return Component.translatable("commands.give.success.single", 1, modelStack.getDisplayName(), pTargets.size());
                }, true);
            }

            return pTargets.size();
        }
    }

}
