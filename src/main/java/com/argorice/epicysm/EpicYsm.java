package com.argorice.epicysm;

import com.argorice.epicysm.compat.YsmProvider;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import com.argorice.epicysm.client.EpicYsmClient;

/** EpicYSM - lets Yes Steve Model characters fight with Epic Fight animations. */
@Mod(EpicYsm.MODID)
public final class EpicYsm {
    public static final String MODID = "epicysm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EpicYsm(IEventBus modBus, ModContainer container) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            YsmProvider.require(net.neoforged.fml.ModList.get()::isLoaded);
            EpicYsmClient.init(modBus, container);
        }
    }
}
