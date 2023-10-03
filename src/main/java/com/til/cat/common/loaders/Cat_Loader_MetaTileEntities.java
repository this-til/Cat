package com.til.cat.common.loaders;

import com.til.cat.common.crafting_type.CraftingType;
import com.til.cat.common.tileentities.machines.GT_MetaTileEntity_Intelligence_Input_ME;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;

public class Cat_Loader_MetaTileEntities {
    protected static int id = 18000;
    public static ItemStack GT_MetaTileEntity_Intelligence_Input_ME_COMPRESSOR;

    public static void register() {
        GT_MetaTileEntity_Intelligence_Input_ME_COMPRESSOR = new GT_MetaTileEntity_Intelligence_Input_ME(
            id++,
            "intelligence.input.me",
            "Intelligence Cat Hatch",
            CraftingType.COMPRESSOR
        ).getStackForm(1);
    }

}
