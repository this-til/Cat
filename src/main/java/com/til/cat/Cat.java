package com.til.cat;

import com.til.cat.common.CommonProxy;

import com.til.cat.common.loaders.Cat_Loader_MetaTileEntities;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

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

    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static final CreativeTabs TAB = new CreativeTabs("Cat Tab") {

        protected final Class<Cat_Loader_MetaTileEntities> cat_loader_metaTileEntitiesClass = Cat_Loader_MetaTileEntities.class;

        @Override
        public Item getTabIconItem() {
            return null;
        }

        @SideOnly(Side.CLIENT)
        public ItemStack getIconItemStack() {
            return Cat_Loader_MetaTileEntities.GT_MetaTileEntity_Intelligence_Input_ME_COMPRESSOR;
        }

        @Override
        public void displayAllReleventItems(List list) {
            super.displayAllReleventItems(list);
            List<ItemStack> itemStackList = (List<ItemStack>) list;
            for (Field declaredField : cat_loader_metaTileEntitiesClass.getDeclaredFields()) {
                if (!Modifier.isStatic(declaredField.getModifiers())) {
                    return;
                }
                if (!declaredField.getType().equals(ItemStack.class)) {
                    continue;
                }
                declaredField.setAccessible(true);
                try {
                    itemStackList.add((ItemStack) declaredField.get(null));
                } catch (IllegalAccessException e) {
                    LOG.warn(e);
                }
            }
        }
    };

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
