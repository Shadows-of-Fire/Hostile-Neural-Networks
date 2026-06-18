package dev.shadowsoffire.hostilenetworks.compat.jade;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.block.DataCenterIOPortBlock;
import dev.shadowsoffire.hostilenetworks.util.IOPortMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(HostileNetworks.MODID)
public class HostileJadePlugin implements IWailaPlugin, IBlockComponentProvider {

    public static final ResourceLocation IO_PORT_MODE = HostileNetworks.loc("io_port_mode");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(this, DataCenterIOPortBlock.class);
        registration.addConfig(IO_PORT_MODE, true);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(IO_PORT_MODE)) return;
        IOPortMode mode = accessor.getBlockState().getValue(DataCenterIOPortBlock.MODE);
        tooltip.add(Component.translatable("hostilenetworks.io_port.mode_set",
            Component.translatable(mode.getTranslationKey())));
    }

    @Override
    public ResourceLocation getUid() {
        return IO_PORT_MODE;
    }
}
