package com.til.cat.common.loaders;

import com.til.cat.common.crafting_type.CraftingType;
import com.til.cat.common.tileentities.machines.GT_MetaTileEntity_Subsidiary_Cat_Hatch;

public class Cat_Loader_MetaTileEntities {
    public static GT_MetaTileEntity_Subsidiary_Cat_Hatch gt_metaTileEntity_subsidiary_cat_hatch;

    public static void init() {
        for (CraftingType value : CraftingType.values()) {
            value.init();
        }
        gt_metaTileEntity_subsidiary_cat_hatch = new GT_MetaTileEntity_Subsidiary_Cat_Hatch(
            18100,
            "subsidiary.cat.hatch",
            "随从猫仓"
        );
    }

    public static void postInit() {
        for (CraftingType value : CraftingType.values()) {
            value.postInit();
        }
    }
}
