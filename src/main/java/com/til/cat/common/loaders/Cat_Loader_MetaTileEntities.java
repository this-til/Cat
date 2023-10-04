package com.til.cat.common.loaders;

import com.til.cat.common.crafting_type.CraftingType;
import com.til.cat.common.tileentities.machines.GT_MetaTileEntity_Intelligence_Input_ME;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;

public class Cat_Loader_MetaTileEntities {

    public static void register() {
        for (CraftingType value : CraftingType.values()) {
            value.register();
        }
    }

}
