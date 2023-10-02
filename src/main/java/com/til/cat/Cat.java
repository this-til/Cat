package com.til.cat;

import com.til.cat.common.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

import java.util.logging.LogManager;
import java.util.logging.Logger;

@Mod(
    modid = Cat.MOD_ID,
    version = Cat.VERSION,
    name = Cat.NOD_NAME,
    dependencies = Cat.DEPENDENCIES,
    acceptedMinecraftVersions = Cat.ACCEPTED_MINECRAFT_VERSIONS)
public class Cat {

    public static final String MOD_ID = "cat";
    public static final String NOD_NAME = "Cat";
    public static final String VERSION = "1.0.0";
    public static final String ACCEPTED_MINECRAFT_VERSIONS = "[1.7.10]";

    public static final String DEPENDENCIES =
        "required-after:IC2;"
        + "required-after:structurelib;"
        + "required-after:modularui;"
        + "after:GalacticraftCore;"
        + "required-after:bartworks;"
        + "after:miscutils;"
        + "after:dreamcraft;"
        + "after:GalacticraftMars;"
        + "after:GalacticraftPlanets";

    //public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @SidedProxy(clientSide = "com.til.cat.client.ClientProxy", serverSide = "com.til.cat.common.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance(Cat.MOD_ID)
    protected static Cat cat;

    public static Cat getInstance() {
        return cat;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
