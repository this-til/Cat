package com.til.cat.common.crafting_type;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.til.cat.common.tileentities.machines.GT_MetaTileEntity_Intelligence_Input_ME;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public enum CraftingType {


    NULL(null),
    /***
     * 组装机
     */
    ASSEMBLER(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes),

    /***
     * 合金炉
     */
    ALLOY_SMELTER(GT_Recipe.GT_Recipe_Map.sAlloySmelterRecipes),

    /***
     * 高压釜
     */
    AUTOCLAVE(GT_Recipe.GT_Recipe_Map.sAutoclaveRecipes),

    /***
     * 卷板机
     */
    BENDING_MACHINE(GT_Recipe.GT_Recipe_Map.sBenderRecipes),

    /***
     * 电路组装机
     */
    CIRCUIT_ASSEMBLER(GT_Recipe.GT_Recipe_Map.sCircuitAssemblerRecipes),

    /***
     * 压缩机配方
     */
    COMPRESSOR(GT_Recipe.GT_Recipe_Map.sCompressorRecipes),

    /***
     * 切割机
     */
    CUTTING_MACHINE(GT_Recipe.GT_Recipe_Map.sCutterRecipes),

    /***
     * 工业高炉
     */
    BLAST(GT_Recipe.GT_Recipe_Map.sBlastRecipes),

    /***
     * 大冰箱
     */
    VACUUM(GT_Recipe.GT_Recipe_Map.sVacuumRecipes),

    /***
     * 化学反应啥啥
     */
    CHEMICAL_RECIPES(GT_Recipe.GT_Recipe_Map.sMultiblockChemicalRecipes),

    /***
     * 提取机
     */
    EXTRACTOR(GT_Recipe.GT_Recipe_Map.sExtractorRecipes),

    /***
     * 压模机
     */
    EXTRUDER(GT_Recipe.GT_Recipe_Map.sExtruderRecipes),

    /***
     * 流体提取机
     */
    FLUID_EXTRACTOR(GT_Recipe.GT_Recipe_Map.sFluidExtractionRecipes),

    /***
     * 流体固化机
     */
    FLUID_SOLIDIFIER(GT_Recipe.GT_Recipe_Map.sFluidSolidficationRecipes),

    /***
     * 表面碾压
     */
    FORMING_PRESS(GT_Recipe.GT_Recipe_Map.sPressRecipes),

    /***
     * 激光雕刻
     */
    LASER_ENGRAVER(GT_Recipe.GT_Recipe_Map.sLaserEngraverRecipes),

    /***
     * 车床
     */
    LATHE(GT_Recipe.GT_Recipe_Map.sLatheRecipes),


    /***
     * 粉碎机
     */
    MACERATOR(GT_Recipe.GT_Recipe_Map.sMaceratorRecipes),

    /***
     * 熔炉
     */
    FURNACE(GT_Recipe.GT_Recipe_Map.sFurnaceRecipes),


    /***
     * 搅拌
     */
    MIXER(GT_Recipe.GT_Recipe_Map.sMixerRecipes),

    /***
     * 磁化机
     */
    ELECTROMAGNETIC_POLARIZER(GT_Recipe.GT_Recipe_Map.sPolarizerRecipes),

    /***
     * 打包机
     */
    PACK(GT_Recipe.GT_Recipe_Map.sBoxinatorRecipes),

    /***
     * 解包机
     */
    UNPACKAGER(GT_Recipe.GT_Recipe_Map.sUnboxinatorRecipes),

    /***
     * 弄线的
     */
    WIREMILL(GT_Recipe.GT_Recipe_Map.sWiremillRecipes),
    ;

    public static final double REDUCE_PROBABILITY_OUT = 0.2;

    private final GT_Recipe.GT_Recipe_Map gtRecipeMap;
    private GT_MetaTileEntity_Intelligence_Input_ME gt_metaTileEntity_intelligence_input_me;
    private ItemStack itemStack;

    private static int id = 18000;

    CraftingType(GT_Recipe.GT_Recipe_Map gtRecipeMap) {
        this.gtRecipeMap = gtRecipeMap;
    }

    public void register() {
        gt_metaTileEntity_intelligence_input_me = new GT_MetaTileEntity_Intelligence_Input_ME(
            id++, "intelligence.input.me",
            "Intelligence Cat Hatch",
            this);
        itemStack = gt_metaTileEntity_intelligence_input_me.getStackForm(1);
    }

    public List<ICraftingPatternDetails> provideCrafting(GT_MetaTileEntity_Intelligence_Input_ME tile, ICraftingProviderHelper craftingProviderHelper) {
        List<ICraftingPatternDetails> list = new ArrayList<>();
        if (gtRecipeMap == null) {
            return list;
        }

        ItemStack[] inputNecessaryItem = tile.getInputNecessaryItem();
        ItemStack[] outNecessaryItem = tile.getOutNecessaryItem();
        FluidStack[] inputNecessaryFluid = tile.getInputNecessaryFluid();
        FluidStack[] outNecessaryFluid = tile.getOutNecessaryFluid();

        boolean hasInputNecessaryItem = inputNecessaryItem[0] != null;
        boolean hasOutNecessaryItem = outNecessaryItem[0] != null;
        boolean hasInputNecessaryFluid = inputNecessaryFluid[0] != null;
        boolean hasOutNecessaryFluid = outNecessaryFluid[0] != null;


        c:
        for (GT_Recipe gt_recipe : gtRecipeMap.mRecipeList) {
            /*if (!hasInputNecessaryItem && needCircuit(gt_recipe.mInputs)) {
                continue;
            }*/
            if (!hasInputNecessaryItem) {
                for (ItemStack mInput : gt_recipe.mInputs) {
                    if (mInput == null) {
                        continue c;
                    }
                    if (mInput.stackSize <= 0) {
                        continue c;
                    }
                }
            }
            if (hasInputNecessaryItem && !hasItem(inputNecessaryItem, gt_recipe.mInputs)) {
                continue;
            }
            if (hasOutNecessaryItem && !hasItem(outNecessaryItem, gt_recipe.mOutputs)) {
                continue;
            }
            if (hasInputNecessaryFluid && !hasFluid(inputNecessaryFluid, gt_recipe.mFluidInputs)) {
                continue;
            }
            if (hasOutNecessaryFluid && !hasFluid(outNecessaryFluid, gt_recipe.mFluidOutputs)) {
                continue;
            }
            boolean hasFluid = false;
            List<IAEItemStack> inAeItemList = new ArrayList<>(gt_recipe.mInputs.length + gt_recipe.mFluidInputs.length);
            for (int i = 0; i < gt_recipe.mInputs.length; i++) {
                if (gt_recipe.mInputs[i] == null) {
                    continue;
                }
                if (gt_recipe.mInputs[i].stackSize <= 0) {
                    continue;
                }
                inAeItemList.add(AEItemStack.create(gt_recipe.mInputs[i]));
            }
            for (int i = 0; i < gt_recipe.mFluidInputs.length; i++) {
                if (gt_recipe.mFluidInputs[i] == null) {
                    continue;
                }
                if (gt_recipe.mFluidInputs[i].amount <= 0) {
                    continue;
                }
                inAeItemList.add(ItemFluidDrop.newAeStack(gt_recipe.mFluidInputs[i]));
                hasFluid = true;
            }
            for (IAEItemStack iaeItemStack : inAeItemList) {
                if (iaeItemStack == null) {
                    continue c;
                }
                iaeItemStack.setStackSize(iaeItemStack.getStackSize() * tile.getMultiple());
            }
            List<IAEItemStack> outAeItemList = new ArrayList<>(gt_recipe.mOutputs.length + gt_recipe.mFluidOutputs.length);
            for (int i = 0; i < gt_recipe.mOutputs.length; i++) {
                if (gt_recipe.mOutputs[i] == null) {
                    continue;
                }
                if (gt_recipe.mOutputs[i].stackSize <= 0) {
                    continue;
                }
                int c = gt_recipe.getOutputChance(i);
                double p = c / 10000d;
                int m = tile.getMultiple();
                if (p < 0) {
                    if (tile.isExcludeProbability()) {
                        break c;
                    }
                    double ap = p * tile.getMultiple();
                    ap *= (1 - REDUCE_PROBABILITY_OUT);
                    if (ap < 0) {
                        break c;
                    }
                    m = (int) Math.floor(ap);
                }
                IAEItemStack iaeItemStack = AEItemStack.create(gt_recipe.mOutputs[i]);
                iaeItemStack.setStackSize(iaeItemStack.getStackSize() * m);
                outAeItemList.add(iaeItemStack);
            }
            for (int i = 0; i < gt_recipe.mFluidOutputs.length; i++) {
                if (gt_recipe.mFluidOutputs[i] == null) {
                    continue;
                }
                if (gt_recipe.mFluidOutputs[i].amount <= 0) {
                    continue;
                }
                int cid = i + gt_recipe.mOutputs.length;
                int c = gt_recipe.getOutputChance(cid);
                double p = c / 10000d;
                int m = tile.getMultiple();
                if (p < 0) {
                    if (tile.isExcludeProbability()) {
                        break c;
                    }
                    double ap = p * tile.getMultiple();
                    ap *= (1 - REDUCE_PROBABILITY_OUT);
                    if (ap < 0) {
                        break c;
                    }
                    m = (int) Math.floor(ap);
                }
                IAEItemStack iaeItemStack = ItemFluidDrop.newAeStack(gt_recipe.mFluidOutputs[i]);
                if (iaeItemStack == null) {
                    break c;
                }
                iaeItemStack.setStackSize(iaeItemStack.getStackSize() * m);
                outAeItemList.add(iaeItemStack);
                hasFluid = true;
            }
            if (inAeItemList.isEmpty()) {
                break;
            }
            if (outAeItemList.isEmpty()) {
                break;
            }
            list.add(new GenerateCraftingPatternDetails(
                inAeItemList.toArray(new IAEItemStack[0]),
                outAeItemList.toArray(new IAEItemStack[0]),
                tile.isCanSubstitute(),
                tile.isCanBeSubstitute(),
                hasFluid
            ));
        }
        return list;
    }

    public GT_Recipe.GT_Recipe_Map getGtRecipeMap() {
        return gtRecipeMap;
    }

    public GT_MetaTileEntity_Intelligence_Input_ME getGt_metaTileEntity_intelligence_input_me() {
        return gt_metaTileEntity_intelligence_input_me;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    /***
     * 获取配方需要的电路id
     */
    public static boolean needCircuit(ItemStack[] in) {
        for (int i = 0; i < in.length; i++) {
            if (GT_Utility.isAnyIntegratedCircuit(in[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFluid(FluidStack[] necessary, FluidStack[] in) {
        c:
        for (FluidStack fluidStack : necessary) {
            if (fluidStack == null) {
                break;
            }
            for (FluidStack stack : in) {
                if (!GT_Utility.areFluidsEqual(fluidStack, stack)) {
                    continue;
                }
                if (stack.amount > 0) {
                    if (stack.amount != fluidStack.amount) {
                        continue;
                    }
                }
                continue c;
            }
            return false;
        }
        return true;

    }

    public static boolean hasItem(ItemStack[] necessary, ItemStack[] in) {
        c:
        for (ItemStack itemStack : necessary) {
            if (itemStack == null) {
                break;
            }
            for (ItemStack stack : in) {
                if (!GT_Utility.areStacksEqual(itemStack, stack)) {
                    continue;
                }
                if (stack.stackSize > 0) {
                    if (stack.stackSize != itemStack.stackSize) {
                        continue;
                    }
                }
                continue c;
            }
            return false;
        }
        return true;
    }
}


