package com.til.cat.common.tileentities.machines;

import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.Color;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.FluidSlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;
import com.til.cat.common.crafting_type.CraftingType;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.gui.modularui.GT_UITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.modularui.IAddUIWidgets;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputInventory;
import ic2.core.util.Vector2;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.*;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_ME_CRAFTING_INPUT_BUFFER;

/***
 * 智能输入仓
 */
public class GT_MetaTileEntity_Intelligence_Input_ME
    extends GT_MetaTileEntity_Hatch
    implements IDualInputHatch, IPowerChannelState, ICraftingProvider, IGridProxyable, IAddUIWidgets, IDualInputInventory {

    public static final int inputNecessaryMaxSlot = 9;
    protected static final int INPUT_NECESSARY_ITEM_WINDOW_ID = 10;
    protected static final int OUT_NECESSARY_ITEM_WINDOW_ID = 11;
    protected static final int INPUT_NECESSARY_FLUID_WINDOW_ID = 12;
    protected static final int OUT_NECESSARY_FLUID_WINDOW_ID = 13;

    protected int multiple = 1;

    protected CraftingType craftingType;

    /***
     * 配方在输入中必须包含的元素
     * 添加电路板，透镜等来区分配方
     */
    protected ItemStack[] inputNecessaryItem = new ItemStack[inputNecessaryMaxSlot];

    /***
     * 在输出中必须包含的元素
     */
    protected ItemStack[] outNecessaryItem = new ItemStack[inputNecessaryMaxSlot];
    /***
     * 配方在输入中必须包含的元素
     * 添加流体来区分润滑液体，高炉保护液体
     */
    protected FluidStack[] inputNecessaryFluid = new FluidStack[inputNecessaryMaxSlot];

    /***
     * 在输出中必须包含的元素
     */
    protected FluidStack[] outNecessaryFluid = new FluidStack[inputNecessaryMaxSlot];

    protected final FluidStackTank[] inputNecessaryFluidTanks = new FluidStackTank[inputNecessaryMaxSlot];
    protected final FluidStackTank[] outNecessaryFluidTanks = new FluidStackTank[inputNecessaryMaxSlot];



    protected List<ItemStack> itemInventory = new ArrayList<>();
    protected List<FluidStack> fluidInventory = new ArrayList<>();

    protected boolean canSubstitute = true;
    protected boolean canBeSubstitute = true;

    @Nullable
    protected AENetworkProxy gridProxy = null;

    protected boolean needPatternSync = true;
    protected boolean justHadNewItems = false;

    protected ArrayList<IDualInputInventory> dualInputInventories = new ArrayList<>();

    {
        dualInputInventories.add(this);
    }

    public GT_MetaTileEntity_Intelligence_Input_ME(int aID, String aName, String aNameRegional, CraftingType craftingType) {
        super(
            aID,
            aName,
            aNameRegional,
            6,
            0,
            new String[]{
                "自动为me网络提供对应合成模板",
                "打开gui进行配置",
            });
        this.craftingType = craftingType;
    }

    public GT_MetaTileEntity_Intelligence_Input_ME(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures, CraftingType craftingType) {
        super(aName, aTier, 0, aDescription, aTextures);
        this.craftingType = craftingType;
        for (int i = 0; i < inputNecessaryMaxSlot; i++) {
            final int index = i;
            inputNecessaryFluidTanks[i] = new FluidStackTank(
                () -> inputNecessaryFluid[index],
                fluid -> inputNecessaryFluid[index] = fluid,
                1);

            outNecessaryFluidTanks[i] = new FluidStackTank(
                () -> outNecessaryFluid[index],
                fluid -> outNecessaryFluid[index] = fluid,
                1);
        }
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return getTexturesInactive(aBaseTexture);
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture,
            TextureFactory.of(OVERLAY_ME_CRAFTING_INPUT_BUFFER)};
    }


    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Intelligence_Input_ME(mName, mTier, mDescriptionArray, mTextures, craftingType);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
        super.onPostTick(aBaseMetaTileEntity, aTimer);

        if (needPatternSync && aTimer % 10 == 0 && getBaseMetaTileEntity().isServerSide()) {
            needPatternSync = !postMEPatternChange();
        }
    }


    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("canSubstitute", canSubstitute);
        aNBT.setBoolean("canBeSubstitute", canBeSubstitute);
        aNBT.setInteger("multiple", multiple);
        aNBT.setString("craftingType", craftingType.name());
        aNBT.setTag("inputNecessaryItem", writeStackArray(inputNecessaryItem));
        aNBT.setTag("outNecessaryItem", writeStackArray(outNecessaryItem));
        aNBT.setTag("inputNecessaryFluid", writeStackArray(inputNecessaryFluid));
        aNBT.setTag("outNecessaryFluid", writeStackArray(outNecessaryFluid));

        NBTTagList itemInventoryNbt = new NBTTagList();
        for (ItemStack itemStack : this.itemInventory) {
            itemInventoryNbt.appendTag(GT_Utility.saveItem(itemStack));
        }
        aNBT.setTag("inventory", itemInventoryNbt);

        NBTTagList fluidInventoryNbt = new NBTTagList();
        for (FluidStack fluidStack : fluidInventory) {
            fluidInventoryNbt.appendTag(fluidStack.writeToNBT(new NBTTagCompound()));
        }
        aNBT.setTag("fluidInventory", fluidInventoryNbt);

        if (GregTech_API.mAE2) {
            getProxy().writeToNBT(aNBT);
        }
    }


    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);

        canSubstitute = aNBT.getBoolean("canSubstitute");
        canBeSubstitute = aNBT.getBoolean("canBeSubstitute");

        multiple = aNBT.getInteger("multiple");
        multiple = Math.max(multiple, 1);
        try {
            craftingType = CraftingType.valueOf("craftingType");
        } catch (Exception e) {
            craftingType = CraftingType.NULL;
        }

        readStackArray(inputNecessaryItem, aNBT.getTagList("inputNecessaryItem", 10));
        readStackArray(outNecessaryItem, aNBT.getTagList("outNecessaryItem", 10));
        readStackArray(inputNecessaryFluid, aNBT.getTagList("inputNecessaryFluid", 10));
        readStackArray(outNecessaryFluid, aNBT.getTagList("outNecessaryFluid", 10));

        itemInventory.clear();
        fluidInventory.clear();
        NBTTagList itemInventoryNbt = aNBT.getTagList("inventory", 10);
        for (int i = 0; i < itemInventoryNbt.tagCount(); i++) {
            itemInventory.add(ItemStack.loadItemStackFromNBT(itemInventoryNbt.getCompoundTagAt(i)));
        }
        NBTTagList fluidInventoryNbt = aNBT.getTagList("fluidInventory", 10);
        for (int i = 0; i < fluidInventoryNbt.tagCount(); i++) {
            fluidInventory.add(FluidStack.loadFluidStackFromNBT(fluidInventoryNbt.getCompoundTagAt(i)));
        }

        if (GregTech_API.mAE2) {
            getProxy().readFromNBT(aNBT);
        }
    }


    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        craftingType.provideCrafting(this, craftingTracker);
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            gridProxy = new AENetworkProxy(this, "proxy", ItemList.Hatch_CraftingInput_Bus_ME.get(1), true);
            gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            if (getBaseMetaTileEntity().getWorld() != null) gridProxy.setOwner(
                getBaseMetaTileEntity().getWorld()
                    .getPlayerEntityByName(getBaseMetaTileEntity().getOwnerName()));
        }

        return this.gridProxy;
    }

    protected boolean postMEPatternChange() {
        // don't post until it's active
        if (!getProxy().isActive()) {
            return false;
        }
        try {
            getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
        } catch (GridAccessException ignored) {
            return false;
        }
        return true;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(
            getBaseMetaTileEntity().getWorld(),
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord());
    }

    @Override
    public boolean justUpdated() {
        boolean ret = justHadNewItems;
        justHadNewItems = false;
        return ret;
    }

    @Override
    public Iterator<IDualInputInventory> inventories() {
        return dualInputInventories.iterator();
    }

    @Override
    public boolean isPowered() {
        return getProxy() != null && getProxy().isPowered();
    }

    @Override
    public boolean isActive() {
        return getProxy() != null && getProxy().isActive();
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (!isActive()) {
            return false;
        }
        if (!insertItemsAndFluids(table)) {
            return false;
        }
        justHadNewItems = true;
        return true;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public ItemStack getCrafterIcon() {
        return getMachineCraftingIcon();
    }

    public boolean insertItemsAndFluids(InventoryCrafting inventoryCrafting) {
        int errorIndex = -1; // overflow may occur at this index
        for (int i = 0; i < inventoryCrafting.getSizeInventory(); ++i) {
            ItemStack itemStack = inventoryCrafting.getStackInSlot(i);
            if (itemStack == null) continue;

            boolean inserted = false;
            if (itemStack.getItem() instanceof ItemFluidPacket) { // insert fluid
                FluidStack fluidStack = ItemFluidPacket.getFluidStack(itemStack);
                if (fluidStack == null) continue;
                for (FluidStack fluid : fluidInventory) {
                    if (!fluid.isFluidEqual(fluidStack)) continue;
                    if (Integer.MAX_VALUE - fluidStack.amount < fluid.amount) {
                        // Overflow detected
                        errorIndex = i;
                        break;
                    }
                    fluid.amount += fluidStack.amount;
                    inserted = true;
                    break;
                }
                if (errorIndex != -1) break;
                if (!inserted) {
                    fluidInventory.add(fluidStack);
                }
            } else { // insert item
                for (ItemStack item : itemInventory) {
                    if (!itemStack.isItemEqual(item)) continue;
                    if (Integer.MAX_VALUE - itemStack.stackSize < item.stackSize) {
                        // Overflow detected
                        errorIndex = i;
                        break;
                    }
                    item.stackSize += itemStack.stackSize;
                    inserted = true;
                    break;
                }
                if (errorIndex != -1) break;
                if (!inserted) {
                    itemInventory.add(itemStack);
                }
            }
        }
        if (errorIndex != -1) { // need to rollback
            // Clean up the inserted items/liquids
            for (int i = 0; i < errorIndex; ++i) {
                ItemStack itemStack = inventoryCrafting.getStackInSlot(i);
                if (itemStack == null) continue;
                if (itemStack.getItem() instanceof ItemFluidPacket) { // remove fluid
                    FluidStack fluidStack = ItemFluidPacket.getFluidStack(itemStack);
                    if (fluidStack == null) continue;
                    for (FluidStack fluid : fluidInventory) {
                        if (fluid.isFluidEqual(fluidStack)) {
                            fluid.amount -= fluidStack.amount;
                            break;
                        }
                    }
                } else { // remove item
                    for (ItemStack item : itemInventory) {
                        if (item.isItemEqual(itemStack)) {
                            item.stackSize -= itemStack.stackSize;
                            break;
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }

    protected boolean isEmptyInventory() {
        if (itemInventory.isEmpty() && fluidInventory.isEmpty()) {
            return true;
        }
        for (ItemStack itemStack : itemInventory) {
            if (itemStack != null && itemStack.stackSize > 0) {
                return false;
            }
        }
        for (FluidStack fluidStack : fluidInventory) {
            if (fluidStack != null && fluidStack.amount > 0) {
                return false;
            }
        }
        return true;
    }


    @Override
    public ItemStack[] getItemInputs() {
        if (isEmptyInventory()) {
            return new ItemStack[0];
        }
        return ArrayUtils.addAll(itemInventory.toArray(new ItemStack[0]), inputNecessaryItem);
    }

    @Override
    public FluidStack[] getFluidInputs() {
        if (isEmptyInventory()) {
            return new FluidStack[0];
        }
        return fluidInventory.toArray(new FluidStack[0]);
    }

    @Override
    public void gridChanged() {
        needPatternSync = true;
    }

    public int getMultiple() {
        return multiple;
    }

    public ItemStack[] getInputNecessaryItem() {
        return inputNecessaryItem;
    }

    public ItemStack[] getOutNecessaryItem() {
        return outNecessaryItem;
    }

    public FluidStack[] getInputNecessaryFluid() {
        return inputNecessaryFluid;
    }

    public FluidStack[] getOutNecessaryFluid() {
        return outNecessaryFluid;
    }

    public boolean isCanSubstitute() {
        return canSubstitute;
    }

    public boolean isCanBeSubstitute() {
        return canBeSubstitute;
    }

    @Override
    public void securityBreak() {

    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return getProxy().getNode();
    }

    @Override
    public void addAdditionalTooltipInformation(ItemStack stack, List<String> tooltip) {
        tooltip.add("机械类型:" + StatCollector.translateToLocal(craftingType.getGtRecipeMap().mUnlocalizedName));
    }


    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        buildContext.addSyncedWindow(INPUT_NECESSARY_ITEM_WINDOW_ID, this::inputNecessaryItemConfigurationWindow);
        buildContext.addSyncedWindow(OUT_NECESSARY_ITEM_WINDOW_ID, this::outNecessaryItemConfigurationWindow);
        buildContext.addSyncedWindow(INPUT_NECESSARY_FLUID_WINDOW_ID, player -> necessaryFluidConfigurationWindow(player, inputNecessaryFluidTanks));
        buildContext.addSyncedWindow(OUT_NECESSARY_FLUID_WINDOW_ID, player -> necessaryFluidConfigurationWindow(player, outNecessaryFluidTanks));
        builder.widget(
            new DrawableWidget().setDrawable(GT_UITextures.PICTURE_ARROW_DOUBLE)
                .setPos(82, 30)
                .setSize(12, 12)
        );
    }

    protected static final Pos2d[] storedPos = new Pos2d[]{
        new Pos2d(7 + 18 * 0, 9 + 18 * 0),
        new Pos2d(7 + 18 * 1, 9 + 18 * 0),
        new Pos2d(7 + 18 * 2, 9 + 18 * 0),
        new Pos2d(7 + 18 * 0, 9 + 18 * 1),
        new Pos2d(7 + 18 * 1, 9 + 18 * 1),
        new Pos2d(7 + 18 * 2, 9 + 18 * 1),
        new Pos2d(7 + 18 * 0, 9 + 18 * 2),
        new Pos2d(7 + 18 * 1, 9 + 18 * 2),
        new Pos2d(7 + 18 * 2, 9 + 18 * 2)
    };

    protected ModularWindow inputNecessaryItemConfigurationWindow(EntityPlayer player) {

        ModularWindow.Builder builder = ModularWindow.builder(getGUIWidth(), getGUIHeight());
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);


        return builder.build();
    }

    protected ModularWindow outNecessaryItemConfigurationWindow(EntityPlayer player) {
        ModularWindow.Builder builder = ModularWindow.builder(getGUIWidth(), getGUIHeight());
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);
        return builder.build();
    }

    protected ModularWindow necessaryFluidConfigurationWindow(EntityPlayer player, FluidStackTank[] fluidStackTanks) {
        ModularWindow.Builder builder = ModularWindow.builder(getGUIWidth(), getGUIHeight());
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);



        for (int i = 0; i < inputNecessaryMaxSlot; i++) {
            Pos2d pos2d = storedPos[i];

        }

        return builder.build();
    }


    public static NBTTagList writeStackArray(ItemStack[] stacks) {
        NBTTagList listTag = new NBTTagList();
        for (ItemStack stack : stacks) {
            NBTTagCompound stackTag = new NBTTagCompound();
            if (stack != null) stack.writeToNBT(stackTag);
            listTag.appendTag(stackTag);
        }
        return listTag;
    }

    public static NBTTagList writeStackArray(FluidStack[] stacks) {
        NBTTagList listTag = new NBTTagList();
        for (FluidStack stack : stacks) {
            NBTTagCompound stackTag = new NBTTagCompound();
            if (stack != null) stack.writeToNBT(stackTag);
            listTag.appendTag(stackTag);
        }
        return listTag;
    }

    public static void readStackArray(ItemStack[] itemStacks, NBTTagList nbtTagList) {
        for (int i = 0; i < itemStacks.length; i++) {
            if (i >= nbtTagList.tagCount()) {
                return;
            }
            itemStacks[i] = ItemStack.loadItemStackFromNBT(nbtTagList.getCompoundTagAt(i));
        }
    }

    public static void readStackArray(FluidStack[] itemStacks, NBTTagList nbtTagList) {
        for (int i = 0; i < itemStacks.length; i++) {
            if (i >= nbtTagList.tagCount()) {
                return;
            }
            itemStacks[i] = FluidStack.loadFluidStackFromNBT(nbtTagList.getCompoundTagAt(i));
        }
    }
}
