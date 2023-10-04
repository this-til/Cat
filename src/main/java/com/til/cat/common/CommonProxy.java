package com.til.cat.common;

import com.til.cat.Config;
import com.til.cat.common.loaders.Cat_Loader_MetaTileEntities;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

    }

    public void init(FMLInitializationEvent event) {
        Cat_Loader_MetaTileEntities.init();
    }

    public void postInit(FMLPostInitializationEvent event) {
        Cat_Loader_MetaTileEntities.postInit();
    }

    public void serverStarting(FMLServerStartingEvent event) {
    }
}
