package com.til.cat.common.tileentities.machines;

import appeng.api.AEApi;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.helpers.IPriorityHost;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.google.common.collect.ImmutableList;
import com.gtnewhorizons.modularui.api.NumberFormat;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.Text;
import com.gtnewhorizons.modularui.api.drawable.TextRenderer;
import com.gtnewhorizons.modularui.api.forge.IItemHandlerModifiable;
import com.gtnewhorizons.modularui.api.forge.ItemStackHandler;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.Color;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;
import com.gtnewhorizons.modularui.common.internal.wrapper.BaseSlot;
import com.gtnewhorizons.modularui.common.widget.*;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;
import com.til.cat.common.crafting_type.CraftingType;
import com.til.cat.common.crafting_type.GenerateCraftingPatternDetails;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.gui.modularui.GT_UIInfos;
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
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_ME_CRAFTING_INPUT_BUFFER;

/***
 * 智能输入仓
 */
public class GT_MetaTileEntity_Intelligence_Cat_Hatch
    extends GT_MetaTileEntity_Hatch
    implements IDualInputHatch, IPowerChannelState, ICraftingProvider,
    IGridProxyable, IAddUIWidgets, IPriorityHost {

    public static final int NECESSARY_MAX_SLOT = 9;
    protected static final int INPUT_NECESSARY_ITEM_WINDOW_ID = 10;
    protected static final int OUT_NECESSARY_ITEM_WINDOW_ID = 11;
    protected static final int INPUT_NECESSARY_FLUID_WINDOW_ID = 12;
    protected static final int OUT_NECESSARY_FLUID_WINDOW_ID = 13;
    protected static final int CONFIGURATION_MULTIPLE_WINDOW_ID = 14;
    protected static final int CONFIGURATION_NUMBER_WINDOW_ID = 15;
    protected static final int CONFIGURATION_PRIORITY_WINDOW = 16;
    protected static final int[] ALL_WINDOW_ID = new int[]{
        INPUT_NECESSARY_ITEM_WINDOW_ID,
        OUT_NECESSARY_ITEM_WINDOW_ID,
        INPUT_NECESSARY_FLUID_WINDOW_ID,
        OUT_NECESSARY_FLUID_WINDOW_ID,
        CONFIGURATION_MULTIPLE_WINDOW_ID,
        CONFIGURATION_NUMBER_WINDOW_ID,
        CONFIGURATION_PRIORITY_WINDOW
    };

    protected static final Pos2d[] storedPos = new Pos2d[]{
        new Pos2d(18 * 0, 18 * 0),
        new Pos2d(18 * 1, 18 * 0),
        new Pos2d(18 * 2, 18 * 0),
        new Pos2d(18 * 0, 18 * 1),
        new Pos2d(18 * 1, 18 * 1),
        new Pos2d(18 * 2, 18 * 1),
        new Pos2d(18 * 0, 18 * 2),
        new Pos2d(18 * 1, 18 * 2),
        new Pos2d(18 * 2, 18 * 2)
    };

    protected int multiple = 1;

    protected int priority;

    @Nullable
    protected CraftingType craftingType;

    /***
     * 配方在输入中必须包含的元素
     * 添加电路板，透镜等来区分配方
     */
    protected ItemStack[] inputNecessaryItem = new ItemStack[NECESSARY_MAX_SLOT];

    /***
     * 在输出中必须包含的元素
     */
    protected ItemStack[] outNecessaryItem = new ItemStack[NECESSARY_MAX_SLOT];
    /***
     * 配方在输入中必须包含的元素
     * 添加流体来区分润滑液体，高炉保护液体
     */
    protected FluidStack[] inputNecessaryFluid = new FluidStack[NECESSARY_MAX_SLOT];

    /***
     * 在输出中必须包含的元素
     */
    protected FluidStack[] outNecessaryFluid = new FluidStack[NECESSARY_MAX_SLOT];

    //protected ItemStackHandler inputNecessaryItemHandler = new ItemStackHandler(inputNecessaryItem);

    //protected ItemStackHandler outNecessaryItemItemHandle = new ItemStackHandler(outNecessaryItem);

    //protected FluidStackTank[] inputNecessaryFluidTanks = new FluidStackTank[NECESSARY_MAX_SLOT];
    //protected FluidStackTank[] outNecessaryFluidTanks = new FluidStackTank[NECESSARY_MAX_SLOT];


    protected List<ItemStack> itemInventory = new ArrayList<>();
    protected List<FluidStack> fluidInventory = new ArrayList<>();


    protected ItemStack[] inputVirtuallyItem = new ItemStack[NECESSARY_MAX_SLOT];

    protected boolean canSubstitute = true;
    protected boolean canBeSubstitute = true;
    protected boolean excludeProbability;

    @Nullable
    protected AENetworkProxy gridProxy = null;
    @Nullable
    protected BaseActionSource requestSource = null;

    protected boolean needPatternSync = true;
    protected boolean justHadNewItems = false;

    protected ArrayList<IDualInputInventory> dualInputInventories = new ArrayList<>();

    {
        dualInputInventories.add(new IDualInputInventory() {
            @Override
            public ItemStack[] getItemInputs() {
                if (isEmptyInventory()) {
                    return new ItemStack[0];
                }
                if (inputVirtuallyItem[0] == null) {
                    return itemInventory.toArray(new ItemStack[0]);
                }
                return ArrayUtils.addAll(itemInventory.toArray(new ItemStack[0]), inputVirtuallyItem);
            }

            @Override
            public FluidStack[] getFluidInputs() {
                if (isEmptyInventory()) {
                    return new FluidStack[0];
                }
                return fluidInventory.toArray(new FluidStack[0]);
            }

        });
    }

    protected List<GenerateCraftingPatternDetails> craftingPatternDetailsList = new ArrayList<>();

    protected HashSet<Integer> disableGtRecipeHasCodeSet = new HashSet<>();


    public GT_MetaTileEntity_Intelligence_Cat_Hatch(int aID, String aName, String aNameRegional, CraftingType craftingType) {
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

    public GT_MetaTileEntity_Intelligence_Cat_Hatch(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures, CraftingType craftingType) {
        super(aName, aTier, 0, aDescription, aTextures);
        this.craftingType = craftingType;
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return getTexturesInactive(aBaseTexture);
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, TextureFactory.of(OVERLAY_ME_CRAFTING_INPUT_BUFFER)};
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        GT_UIInfos.openGTTileEntityUI(aBaseMetaTileEntity, aPlayer);
        return true;
    }

    @Override
    public void onLeftclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (!(aPlayer instanceof EntityPlayerMP)) {
            return;
        }

        ItemStack dataStick = aPlayer.inventory.getCurrentItem();
        if (!ItemList.Tool_DataStick.isStackEqual(dataStick, true, true)) {
            return;
        }

        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", "Intelligence Cat Hatch");
        tag.setInteger("x", aBaseMetaTileEntity.getXCoord());
        tag.setInteger("y", aBaseMetaTileEntity.getYCoord());
        tag.setInteger("z", aBaseMetaTileEntity.getZCoord());

        dataStick.stackTagCompound = tag;
        dataStick.setStackDisplayName(
            "捕获一只猫猫 (" + aBaseMetaTileEntity
                .getXCoord() + ", " + aBaseMetaTileEntity.getYCoord() + ", " + aBaseMetaTileEntity.getZCoord() + ")");
        aPlayer.addChatMessage(new ChatComponentText("恭喜猫德学院抓获狮子猫！！"));
    }

    @Override
    public boolean useModularUI() {
        return true;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        getProxy().onReady();
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side, ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side, ItemStack aStack) {
        return false;
    }

    @Override
    public int getCircuitSlot() {
        return 0;
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return false;
    }


    @Override
    public AECableType getCableConnectionType(ForgeDirection forgeDirection) {
        return isOutputFacing(forgeDirection) ? AECableType.SMART : AECableType.NONE;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Intelligence_Cat_Hatch(mName, mTier, mDescriptionArray, mTextures, craftingType);
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
        aNBT.setBoolean("excludeProbability", excludeProbability);
        aNBT.setInteger("multiple", multiple);
        aNBT.setString("craftingType", craftingType.name());
        aNBT.setTag("inputNecessaryItem", writeStackArray(inputNecessaryItem));
        aNBT.setTag("outNecessaryItem", writeStackArray(outNecessaryItem));
        aNBT.setTag("inputNecessaryFluid", writeStackArray(inputNecessaryFluid));
        aNBT.setTag("outNecessaryFluid", writeStackArray(outNecessaryFluid));
        aNBT.setInteger("priority", priority);
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

        int[] disableGtRecipeHasCodeArray = new int[disableGtRecipeHasCodeSet.size()];
        int i = 0;
        for (Integer integer : disableGtRecipeHasCodeSet) {
            disableGtRecipeHasCodeArray[i] = integer;
            i++;
        }
        aNBT.setIntArray("disableGtRecipeHasCodeSet", disableGtRecipeHasCodeArray);

        if (GregTech_API.mAE2) {
            getProxy().writeToNBT(aNBT);
        }
    }


    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);

        canSubstitute = aNBT.getBoolean("canSubstitute");
        canBeSubstitute = aNBT.getBoolean("canBeSubstitute");
        excludeProbability = aNBT.getBoolean("excludeProbability");
        priority = aNBT.getInteger("priority");

        multiple = aNBT.getInteger("multiple");
        multiple = Math.max(multiple, 1);
        try {
            craftingType = CraftingType.valueOf(aNBT.getString("craftingType"));
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

        disableGtRecipeHasCodeSet.clear();
        for (int gtRecipeHasCodeSet : aNBT.getIntArray("disableGtRecipeHasCodeSet")) {
            disableGtRecipeHasCodeSet.add(gtRecipeHasCodeSet);
        }

        if (GregTech_API.mAE2) {
            getProxy().readFromNBT(aNBT);
        }
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (craftingType == null) {
            return;
        }
        craftingPatternDetailsList = craftingType.provideCrafting(this, craftingTracker);
        for (GenerateCraftingPatternDetails iCraftingPatternDetails : craftingPatternDetailsList) {
            if (disableGtRecipeHasCodeSet.contains(iCraftingPatternDetails.getGtRecipeHasCode())) {
                continue;
            }
            craftingTracker.addCraftingOption(this, iCraftingPatternDetails);
        }
        for (int i = 0; i < NECESSARY_MAX_SLOT; i++) {
            if (inputNecessaryItem[i] == null || inputNecessaryItem[i].stackSize != 0) {
                inputVirtuallyItem[i] = null;
            }
            inputVirtuallyItem[i] = inputNecessaryItem[i];
        }
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            gridProxy = new AENetworkProxy(this, "proxy", getStackInSlot(1), true);
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

    protected BaseActionSource getRequest() {
        if (requestSource == null) {
            requestSource = new MachineSource((IActionHost) getBaseMetaTileEntity());
        }
        return requestSource;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int newValue) {
        this.priority = newValue;
    }

    protected void refundAll() throws GridAccessException {
        if (!isActive()) {
            return;
        }
        AENetworkProxy proxy = getProxy();
        BaseActionSource baseActionSource = getRequest();
        IMEMonitor<IAEItemStack> sg = proxy.getStorage()
            .getItemInventory();
        for (ItemStack itemStack : itemInventory) {
            if (itemStack == null || itemStack.stackSize == 0) continue;
            IAEItemStack rest = Platform.poweredInsert(
                proxy.getEnergy(),
                sg,
                AEApi.instance()
                    .storage()
                    .createItemStack(itemStack),
                baseActionSource);
            itemStack.stackSize = rest != null && rest.getStackSize() > 0 ? (int) rest.getStackSize() : 0;
        }
        IMEMonitor<IAEFluidStack> fsg = proxy.getStorage()
            .getFluidInventory();
        for (FluidStack fluidStack : fluidInventory) {
            if (fluidStack == null || fluidStack.amount == 0) continue;
            IAEFluidStack rest = Platform.poweredInsert(
                proxy.getEnergy(),
                fsg,
                AEApi.instance()
                    .storage()
                    .createFluidStack(fluidStack),
                baseActionSource);
            fluidStack.amount = rest != null && rest.getStackSize() > 0 ? (int) rest.getStackSize() : 0;
        }
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
    public void onBlockDestroyed() {
        try {
            refundAll();
        } catch (GridAccessException ignored) {
        }
        super.onBlockDestroyed();
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

    public boolean isExcludeProbability() {
        return excludeProbability;
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
        tooltip.add("机械类型:" + (craftingType == null ? "null" : StatCollector.translateToLocal(craftingType.getGtRecipeMap().mUnlocalizedName)));
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        NBTTagCompound tag = accessor.getNBTData();
        currenttip.add("机械类型:" + (tag.hasKey("craftingType") ? StatCollector.translateToLocal(tag.getString("craftingType")) : "null"));
        if (tag.hasKey("inventory")) {
            NBTTagList inventory = tag.getTagList("inventory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < inventory.tagCount(); ++i) {
                NBTTagCompound item = inventory.getCompoundTagAt(i);
                String name = item.getString("name");
                int amount = item.getInteger("amount");
                currenttip.add(
                    name + ": "
                    + EnumChatFormatting.GOLD
                    + ReadableNumberConverter.INSTANCE.toWideReadableForm(amount)
                    + EnumChatFormatting.RESET);
            }
        }
        super.getWailaBody(itemStack, currenttip, accessor, config);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y, int z) {
        tag.setString("craftingType", craftingType == null ? "null" : craftingType.getGtRecipeMap().mUnlocalizedName);


        NBTTagList inventory = new NBTTagList();
        HashMap<String, Integer> nameToAmount = new HashMap<>();

        for (ItemStack itemStack : itemInventory) {
            if (itemStack != null && itemStack.stackSize > 0) {
                String name = itemStack.getDisplayName();
                int amount = nameToAmount.getOrDefault(name, 0);
                nameToAmount.put(name, amount + itemStack.stackSize);
            }
        }
        for (FluidStack fluidStack : fluidInventory) {
            if (fluidStack != null && fluidStack.amount > 0) {
                String name = fluidStack.getLocalizedName();
                int amount = nameToAmount.getOrDefault(name, 0);
                nameToAmount.put(name, amount + fluidStack.amount);
            }
        }

        for (Map.Entry<String, Integer> entry : nameToAmount.entrySet()) {
            NBTTagCompound item = new NBTTagCompound();
            item.setString("name", entry.getKey());
            item.setInteger("amount", entry.getValue());
            inventory.appendTag(item);
        }
        tag.setTag("inventory", inventory);
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
    }


    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        buildContext.addSyncedWindow(INPUT_NECESSARY_ITEM_WINDOW_ID, player -> createNecessaryItemConfigurationWindow(player, inputNecessaryItem, GT_UITextures.PICTURE_ITEM_IN));
        buildContext.addSyncedWindow(OUT_NECESSARY_ITEM_WINDOW_ID, player -> createNecessaryItemConfigurationWindow(player, outNecessaryItem, GT_UITextures.PICTURE_ITEM_OUT));
        buildContext.addSyncedWindow(INPUT_NECESSARY_FLUID_WINDOW_ID, player -> createNecessaryFluidConfigurationWindow(player, inputNecessaryFluid, GT_UITextures.PICTURE_FLUID_IN));
        buildContext.addSyncedWindow(OUT_NECESSARY_FLUID_WINDOW_ID, player -> createNecessaryFluidConfigurationWindow(player, outNecessaryFluid, GT_UITextures.PICTURE_FLUID_OUT));
        buildContext.addSyncedWindow(CONFIGURATION_MULTIPLE_WINDOW_ID, this::createConfigurationMultipleWindow);
        buildContext.addSyncedWindow(CONFIGURATION_NUMBER_WINDOW_ID, this::createConfigurationNumberWindow);
        buildContext.addSyncedWindow(CONFIGURATION_PRIORITY_WINDOW, this::createConfigurationPriorityWindow);
        {
            ButtonWidget itemInButtonWidget = new ButtonWidget();

            itemInButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    closeAllWindow(widget);
                    widget.getContext().openSyncedWindow(INPUT_NECESSARY_ITEM_WINDOW_ID);
                }
            });
            itemInButtonWidget.setPlayClickSound(true);
            itemInButtonWidget.setBackground(() -> itemInButtonWidget.getContext().isWindowOpen(INPUT_NECESSARY_ITEM_WINDOW_ID) ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.PICTURE_ITEM_IN}
                : new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.PICTURE_ITEM_IN});
            itemInButtonWidget.addTooltips(ImmutableList.of("打开输入物品限制配置窗口"));
            itemInButtonWidget.setSize(16, 16);
            itemInButtonWidget.setPos(7 + 18 * 0, 9);

            builder.widget(itemInButtonWidget);
        }

        {
            ButtonWidget itemOutButtonWidget = new ButtonWidget();

            itemOutButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    closeAllWindow(widget);
                    widget.getContext().openSyncedWindow(OUT_NECESSARY_ITEM_WINDOW_ID);
                }
            });
            itemOutButtonWidget.setPlayClickSound(true);
            itemOutButtonWidget.setBackground(() -> itemOutButtonWidget.getContext().isWindowOpen(OUT_NECESSARY_ITEM_WINDOW_ID) ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.PICTURE_ITEM_OUT}
                : new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.PICTURE_ITEM_OUT});
            itemOutButtonWidget.addTooltips(ImmutableList.of("打开输出物品限制配置窗口"));
            itemOutButtonWidget.setSize(16, 16);
            itemOutButtonWidget.setPos(7 + 18 * 1, 9);
            builder.widget(itemOutButtonWidget);
        }

        {


            ButtonWidget fluidInButtonWidget = new ButtonWidget();

            fluidInButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    closeAllWindow(widget);
                    widget.getContext().openSyncedWindow(INPUT_NECESSARY_FLUID_WINDOW_ID);
                }
            });
            fluidInButtonWidget.setPlayClickSound(true);
            fluidInButtonWidget.setBackground(() -> fluidInButtonWidget.getContext().isWindowOpen(INPUT_NECESSARY_FLUID_WINDOW_ID) ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.PICTURE_FLUID_IN}
                : new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.PICTURE_FLUID_IN});
            fluidInButtonWidget.addTooltips(ImmutableList.of("打开输入流体限制配置窗口"));
            fluidInButtonWidget.setSize(16, 16);
            fluidInButtonWidget.setPos(7 + 18 * 2, 9);

            builder.widget(fluidInButtonWidget);
        }

        {

            ButtonWidget fluidOutButtonWidget = new ButtonWidget();

            fluidOutButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    closeAllWindow(widget);
                    widget.getContext().openSyncedWindow(OUT_NECESSARY_FLUID_WINDOW_ID);
                }
            });
            fluidOutButtonWidget.setPlayClickSound(true);
            fluidOutButtonWidget.setBackground(() -> fluidOutButtonWidget.getContext().isWindowOpen(OUT_NECESSARY_FLUID_WINDOW_ID) ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.PICTURE_FLUID_OUT}
                : new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.PICTURE_FLUID_OUT});
            fluidOutButtonWidget.addTooltips(ImmutableList.of("打开输出流体限制配置窗口"));
            fluidOutButtonWidget.setSize(16, 16);
            fluidOutButtonWidget.setPos(7 + 18 * 3, 9);

            builder.widget(fluidOutButtonWidget);
        }

        {


            ButtonWidget multipleButtonWidget = new ButtonWidget();
            multipleButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    closeAllWindow(widget);
                    widget.getContext().openSyncedWindow(CONFIGURATION_MULTIPLE_WINDOW_ID);
                }
            });
            multipleButtonWidget.setPlayClickSound(true);
            multipleButtonWidget.setBackground(() -> multipleButtonWidget.getContext().isWindowOpen(CONFIGURATION_MULTIPLE_WINDOW_ID) ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.OVERLAY_BUTTON_DOWN_TIERING_OFF}
                : new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.OVERLAY_BUTTON_DOWN_TIERING_OFF});
            multipleButtonWidget.addTooltips(ImmutableList.of(
                "打开样板放大参数配置窗口",
                "根据此处设置参数按比例放大输入输出物品/流体数量",
                "遇到概率输出的物品/流体将自动计算概率总和(基础概率*放大参数*0.8)",
                "如果概率总和小于1将取消该配方构建的样板",
                "样板使用的放大参数为放大参数的向下取整",
                "调高板放大参数可以增强配方的稳定性",
                "你也可以单独设置排除概率输出的配方"));
            multipleButtonWidget.setSize(16, 16);
            multipleButtonWidget.setPos(7 + 18 * 4, 9);

            builder.widget(multipleButtonWidget);
        }

        {
            ButtonWidget buttonWidget = new ButtonWidget();
            buttonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    closeAllWindow(widget);
                    widget.getContext().openSyncedWindow(CONFIGURATION_PRIORITY_WINDOW);
                }
            });
            buttonWidget.setPlayClickSound(true);
            buttonWidget.setBackground(() -> buttonWidget.getContext().isWindowOpen(CONFIGURATION_PRIORITY_WINDOW) ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.OVERLAY_BUTTON_BATCH_MODE_ON}
                : new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.OVERLAY_BUTTON_BATCH_MODE_ON});
            buttonWidget.addTooltips(ImmutableList.of("配置优先级"));
            buttonWidget.setSize(16, 16);
            buttonWidget.setPos(7 + 18 * 5, 9);


            builder.widget(buttonWidget);
        }

        {
            ButtonWidget exportButtonWidget = new ButtonWidget();
            exportButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    try {
                        refundAll();
                    } catch (GridAccessException ignored) {
                    }
                }
            });
            exportButtonWidget.setPlayClickSound(true);
            exportButtonWidget.setBackground(GT_UITextures.BUTTON_STANDARD, GT_UITextures.BUTTON_STANDARD, GT_UITextures.OVERLAY_BUTTON_EXPORT);
            exportButtonWidget.addTooltips(ImmutableList.of("返回所有物品到AE"));
            exportButtonWidget.setSize(16, 16);
            exportButtonWidget.setPos(7 + 18 * 6, 9);


            builder.widget(exportButtonWidget);
        }

        {
            ButtonWidget buttonWidget = new ButtonWidget();
            buttonWidget.setOnClick((clickData, widget) -> {
                canSubstitute = !canSubstitute;
                needPatternSync = true;
            });
            buttonWidget.setBackground(() -> canSubstitute ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.OVERLAY_BUTTON_CHECKMARK} :
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.OVERLAY_BUTTON_CROSS});
            buttonWidget.setPlayClickSound(true);
            buttonWidget.addTooltips(ImmutableList.of("生成ae样板的将启用替换功能"));
            buttonWidget.setSize(16, 16);
            buttonWidget.setPos(7 + 18 * 0, 9 + 18 * 1);

            builder.widget(buttonWidget);
        }

        {
            ButtonWidget buttonWidget = new ButtonWidget();
            buttonWidget.setOnClick((clickData, widget) -> {
                canBeSubstitute = !canBeSubstitute;
                needPatternSync = true;
            });
            buttonWidget.setBackground(() -> canBeSubstitute ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.OVERLAY_BUTTON_CHECKMARK} :
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.OVERLAY_BUTTON_CROSS});
            buttonWidget.setPlayClickSound(true);
            buttonWidget.addTooltips(ImmutableList.of("生成ae样板的将启用被替换功能"));
            buttonWidget.setSize(16, 16);
            buttonWidget.setPos(7 + 18 * 1, 9 + 18 * 1);

            builder.widget(buttonWidget);
        }

        {
            ButtonWidget buttonWidget = new ButtonWidget();
            buttonWidget.setOnClick((clickData, widget) -> {
                excludeProbability = !excludeProbability;
                needPatternSync = true;
            });
            buttonWidget.setBackground(() -> excludeProbability ?
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD_PRESSED, GT_UITextures.OVERLAY_BUTTON_CHECKMARK} :
                new IDrawable[]{GT_UITextures.BUTTON_STANDARD, GT_UITextures.OVERLAY_BUTTON_CROSS});
            buttonWidget.setPlayClickSound(true);
            buttonWidget.addTooltips(ImmutableList.of("如果启用将直接排除有概率输出的配方样板"));
            buttonWidget.setSize(16, 16);
            buttonWidget.setPos(7 + 18 * 2, 9 + 18 * 1);

            builder.widget(buttonWidget);
        }

    }

    @Nullable
    protected Consumer<Integer> setAmount;

    protected int inAmount;

    protected ModularWindow createConfigurationNumberWindow(final EntityPlayer player) {
        final int WIDTH = 78;
        final int HEIGHT = 40;
        final int PARENT_WIDTH = getGUIWidth();
        final int PARENT_HEIGHT = getGUIHeight();
        ModularWindow.Builder builder = ModularWindow.builder(WIDTH, HEIGHT);
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);
        builder.setPos(
            (size, window) -> Alignment.Center.getAlignedPos(
                    size,
                    new Size(PARENT_WIDTH, PARENT_HEIGHT))
                .add(Alignment.TopRight.getAlignedPos(new Size(PARENT_WIDTH, PARENT_HEIGHT), new Size(WIDTH, HEIGHT)).add(WIDTH - 3, -0))
                .add(0, 60));
        builder.widget(
                new TextWidget("数量").setPos(3, 2)
                    .setSize(74, 14))
            .widget(
                new TextFieldWidget().setSetterInt(val -> {
                        if (setAmount != null) {
                            setAmount.accept(val);
                            needPatternSync = true;
                        }
                    })
                    .setGetterInt(() -> inAmount)
                    .setNumbers(0, Integer.MAX_VALUE)
                    .setOnScrollNumbers(1, 1, 64)
                    .setTextAlignment(Alignment.Center)
                    .setTextColor(Color.WHITE.normal)
                    .setSize(36, 18)
                    .setPos(19, 18)
                    .setBackground(GT_UITextures.BACKGROUND_TEXT_FIELD));
        return builder.build();

    }

    protected ModularWindow createConfigurationPriorityWindow(final EntityPlayer player) {
        final int WIDTH = 78;
        final int HEIGHT = 40;
        final int PARENT_WIDTH = getGUIWidth();
        final int PARENT_HEIGHT = getGUIHeight();
        ModularWindow.Builder builder = ModularWindow.builder(WIDTH, HEIGHT);
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);
        builder.setPos(
            (size, window) -> Alignment.Center.getAlignedPos(size, new Size(PARENT_WIDTH, PARENT_HEIGHT))
                .add(
                    Alignment.TopRight.getAlignedPos(new Size(PARENT_WIDTH, PARENT_HEIGHT), new Size(WIDTH, HEIGHT))
                        .add(WIDTH - 3, 0)));
        builder.widget(
                new TextWidget("优先级").setPos(3, 2)
                    .setSize(74, 14))
            .widget(
                new TextFieldWidget().setSetterInt(val -> {
                        multiple = val;
                        needPatternSync = true;
                    })
                    .setGetterInt(() -> multiple)
                    .setNumbers(Integer.MIN_VALUE, Integer.MAX_VALUE)
                    .setOnScrollNumbers(1, 1, 64)
                    .setTextAlignment(Alignment.Center)
                    .setTextColor(Color.WHITE.normal)
                    .setSize(36, 18)
                    .setPos(19, 18)
                    .setBackground(GT_UITextures.BACKGROUND_TEXT_FIELD));
        return builder.build();
    }

    protected ModularWindow createConfigurationMultipleWindow(final EntityPlayer player) {
        final int WIDTH = 78;
        final int HEIGHT = 40;
        final int PARENT_WIDTH = getGUIWidth();
        final int PARENT_HEIGHT = getGUIHeight();
        ModularWindow.Builder builder = ModularWindow.builder(WIDTH, HEIGHT);
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);
        builder.setPos(
            (size, window) -> Alignment.Center.getAlignedPos(size, new Size(PARENT_WIDTH, PARENT_HEIGHT))
                .add(
                    Alignment.TopRight.getAlignedPos(new Size(PARENT_WIDTH, PARENT_HEIGHT), new Size(WIDTH, HEIGHT))
                        .add(WIDTH - 3, 0)));
        builder.widget(
                new TextWidget("放大倍数").setPos(3, 2)
                    .setSize(74, 14))
            .widget(
                new TextFieldWidget().setSetterInt(val -> {
                        multiple = val;
                        needPatternSync = true;
                    })
                    .setGetterInt(() -> multiple)
                    .setNumbers(1, 65536)
                    .setOnScrollNumbers(1, 1, 64)
                    .setTextAlignment(Alignment.Center)
                    .setTextColor(Color.WHITE.normal)
                    .setSize(36, 18)
                    .setPos(19, 18)
                    .setBackground(GT_UITextures.BACKGROUND_TEXT_FIELD));
        return builder.build();
    }


    protected ModularWindow createNecessaryItemConfigurationWindow(EntityPlayer player, ItemStack[] necessaryItemStack, IDrawable iDrawable) {
        final int WIDTH = 60;
        final int HEIGHT = 60;
        final int PARENT_WIDTH = getGUIWidth();
        final int PARENT_HEIGHT = getGUIHeight();
        ModularWindow.Builder builder = ModularWindow.builder(WIDTH, HEIGHT);
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);
        builder.setPos(
            (size, window) -> Alignment.Center.getAlignedPos(
                size,
                new Size(PARENT_WIDTH, PARENT_HEIGHT)
            ).add(Alignment.TopRight.getAlignedPos(
                new Size(PARENT_WIDTH, PARENT_HEIGHT),
                new Size(WIDTH, HEIGHT)).add(WIDTH - 3, 0)));
        SlotWidget[] slotWidgets = new SlotWidget[NECESSARY_MAX_SLOT];
        IItemHandlerModifiable inputNecessaryItemHandler = new ItemStackHandler(necessaryItemStack);
        SlotGroup.ItemGroupBuilder itemGroupBuilder = SlotGroup.ofItemHandler(inputNecessaryItemHandler, 3);
        itemGroupBuilder.startFromSlot(0);
        itemGroupBuilder.endAtSlot(8);
        itemGroupBuilder.phantom(true);
        itemGroupBuilder.background(getGUITextureSet().getItemSlot(), iDrawable);
        itemGroupBuilder.slotCreator(i -> new BaseSlot(inputNecessaryItemHandler, i, true) {
            @Override
            public void putStack(ItemStack stack) {
                if (GT_Utility.areStacksEqual(necessaryItemStack[i], stack)) {
                    if (necessaryItemStack[i].stackSize == stack.stackSize) {
                        return;
                    }
                }
                if (necessaryItemStack[i] == null && stack != null) {
                    stack = GT_Utility.copyAmount(0L, stack);
                }
                necessaryItemStack[i] = stack;
                needPatternSync = true;
                slotWidgets[i].detectAndSendChanges(true);
            }
        });
        itemGroupBuilder.widgetCreator(slot -> {
            SlotWidget slotWidget = new SlotWidget(slot) {
                @Override
                protected void phantomClick(ClickData clickData, ItemStack cursorStack) {

                    switch (clickData.mouseButton) {
                        case 0:
                        case 1:
                            if (cursorStack == null) {
                                if (necessaryItemStack[slot.getSlotIndex()] == null) {
                                    break;
                                }
                                slotWidgets[slot.getSlotIndex()].getMcSlot().putStack(null);
                                for (int i = slot.getSlotIndex(); i < NECESSARY_MAX_SLOT - 1; i++) {
                                    slotWidgets[i].getMcSlot().putStack(necessaryItemStack[i + 1]);
                                }
                                if (necessaryItemStack[NECESSARY_MAX_SLOT - 1] != null) {
                                    slotWidgets[NECESSARY_MAX_SLOT - 1].getMcSlot().putStack(null);
                                }
                                break;
                            }
                            ItemStack addItemStack = GT_Utility.copyAmount(0L, cursorStack);
                            for (int i = 0; i < NECESSARY_MAX_SLOT; i++) {
                                if (necessaryItemStack[i] == null) {
                                    slotWidgets[i].getMcSlot().putStack(addItemStack);
                                    slotWidgets[i].detectAndSendChanges(true);
                                    break;
                                }
                                if (GT_Utility.areStacksEqual(necessaryItemStack[i], addItemStack)) {
                                    break;
                                }
                            }
                            break;
                        case 2:
                            if (getContext().isClient()) {
                                break;
                            }
                            if (necessaryItemStack[slot.getSlotIndex()] == null) {
                                break;
                            }
                            if (getContext().isWindowOpen(CONFIGURATION_NUMBER_WINDOW_ID)) {
                                getContext().closeWindow(CONFIGURATION_NUMBER_WINDOW_ID);
                            }
                            getContext().openSyncedWindow(CONFIGURATION_NUMBER_WINDOW_ID);
                            setAmount = a -> {
                                slotWidgets[slot.getSlotIndex()].getMcSlot().putStack(GT_Utility.copyAmount(a, necessaryItemStack[slot.getSlotIndex()]));
                                needPatternSync = true;
                                inAmount = a;
                            };
                            inAmount = necessaryItemStack[slot.getSlotIndex()].stackSize;
                            break;
                    }

                }

                @Override
                public void buildTooltip(List<Text> tooltip) {
                    super.buildTooltip(tooltip);
                    if (necessaryItemStack[slot.getSlotIndex()] != null) {
                        tooltip.add(Text.localised("modularui.item.amount", necessaryItemStack[slot.getSlotIndex()].stackSize, 64));
                    }
                }

            };
            //slotWidget.getMcSlot().putStack(necessaryItemStack[slot.getSlotIndex()]);
            slotWidgets[slot.getSlotIndex()] = slotWidget;
            return slotWidget;
        });
        builder.widget(itemGroupBuilder.build().setPos(3, 3));
        return builder.build();
    }

    protected ModularWindow createNecessaryFluidConfigurationWindow(EntityPlayer player, FluidStack[] necessaryFluidStack, IDrawable iDrawable) {
        final int WIDTH = 60;
        final int HEIGHT = 60;
        final int PARENT_WIDTH = getGUIWidth();
        final int PARENT_HEIGHT = getGUIHeight();
        ModularWindow.Builder builder = ModularWindow.builder(WIDTH, HEIGHT);
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);
        builder.setPos(
            (size, window) -> Alignment.Center.getAlignedPos(
                size,
                new Size(PARENT_WIDTH, PARENT_HEIGHT)
            ).add(Alignment.TopRight.getAlignedPos(
                new Size(PARENT_WIDTH, PARENT_HEIGHT),
                new Size(WIDTH, HEIGHT)).add(WIDTH - 3, 0)));
        FluidSlotWidget[] fluidSlotWidgets = new FluidSlotWidget[NECESSARY_MAX_SLOT];
        Field field = null;
        try {
            field = FluidSlotWidget.class.getDeclaredField("phantom");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
        for (int i = 0; i < NECESSARY_MAX_SLOT; i++) {
            int finalI = i;
            FluidSlotWidget fluidSlotWidget = new FluidSlotWidget(new FluidStackTank(
                () -> necessaryFluidStack[finalI],
                fluid -> necessaryFluidStack[finalI] = fluid,
                Integer.MAX_VALUE
            )) {
                static final int PACKET_EMPTY_CLICK = 6;

                @Override
                public ClickResult onClick(int buttonId, boolean doubleClick) {
                    ItemStack cursorStack = getContext().getCursor().getItemStack();
                    if (cursorStack == null) {
                        ClickData clickData = ClickData.create(buttonId, doubleClick);
                        syncToServer(PACKET_EMPTY_CLICK, clickData::writeToPacket);
                        return ClickResult.ACCEPT;
                    }
                    return super.onClick(buttonId, doubleClick);
                }

                @Override
                public void readOnServer(int id, PacketBuffer buf) throws IOException {
                    super.readOnServer(id, buf);
                    switch (id) {
                        case PACKET_EMPTY_CLICK:
                            ClickData clickData = ClickData.readPacket(buf);
                            switch (clickData.mouseButton) {
                                case 0:
                                case 1:
                                    clearTag();
                                    break;
                                case 2:
                                    if (necessaryFluidStack[finalI] == null) {
                                        break;
                                    }
                                    if (getContext().isWindowOpen(CONFIGURATION_NUMBER_WINDOW_ID)) {
                                        getContext().closeWindow(CONFIGURATION_NUMBER_WINDOW_ID);
                                    }
                                    getContext().openSyncedWindow(CONFIGURATION_NUMBER_WINDOW_ID);
                                    setAmount = a -> {
                                        necessaryFluidStack[finalI] = GT_Utility.copyAmount(a, necessaryFluidStack[finalI]);
                                        needPatternSync = true;
                                        inAmount = a;
                                        detectAndSendChangesAll();
                                    };
                                    inAmount = necessaryFluidStack[finalI].amount;
                                    break;
                            }

                    }
                    markForUpdate();
                }

                private void clearTag() {
                    if (necessaryFluidStack[finalI] == null) {
                        return;
                    }

                    necessaryFluidStack[finalI] = null;
                    needPatternSync = true;
                    for (int i = finalI; i < NECESSARY_MAX_SLOT - 1; i++) {
                        necessaryFluidStack[i] = necessaryFluidStack[i + 1];
                    }
                    if (necessaryFluidStack[NECESSARY_MAX_SLOT - 1] != null) {
                        necessaryFluidStack[NECESSARY_MAX_SLOT - 1] = null;
                    }
                    detectAndSendChangesAll();
                }

                @Override
                protected void onClickServer(ClickData clickData, ItemStack clientVerifyToken) {
                    EntityPlayer player = getContext().getPlayer();
                    ItemStack heldItem = player.inventory.getItemStack();
                    if (clickData.mouseButton != 0) {
                        return;
                    }
                    if (heldItem == null) {
                        clearTag();
                        return;
                    }
                    FluidStack heldFluid = getFluidForPhantomItem(heldItem);
                    if (heldFluid == null) {
                        return;
                    }
                    FluidStack setFileStack = GT_Utility.copyAmount(0, heldFluid);
                    for (int ii = 0; ii < NECESSARY_MAX_SLOT; ii++) {
                        if (necessaryFluidStack[ii] == null) {
                            necessaryFluidStack[ii] = setFileStack;
                            needPatternSync = true;
                            fluidSlotWidgets[ii].detectAndSendChanges(true);
                            break;
                        }
                        if (GT_Utility.areFluidsEqual(necessaryFluidStack[ii], setFileStack)) {
                            break;
                        }
                    }
                }

                @Override
                protected void tryScrollPhantom(int direction) {
                }

                @Override
                public void buildTooltip(List<Text> tooltip) {
                    super.buildTooltip(tooltip);
                    if (necessaryFluidStack[finalI] != null) {
                        tooltip.add(Text.localised("modularui.fluid.amount", necessaryFluidStack[finalI].amount, Integer.MAX_VALUE));
                    }
                }

                private TextRenderer textRenderer;

                @Override
                public void draw(float partialTicks) {
                    super.draw(partialTicks);
                    if (necessaryFluidStack[finalI] != null && necessaryFluidStack[finalI].amount != 0) {
                        String s = NumberFormat.formatLong(necessaryFluidStack[finalI].amount) + "L";
                        textRenderer.setAlignment(Alignment.CenterLeft, size.width - getContentOffset().x - 1f);
                        textRenderer.setPos((int) (getContentOffset().x + 0.5f), (int) (size.height - 4.5f));
                        textRenderer.draw(s);
                    }
                }

                @Override
                public void onInit() {
                    super.onInit();
                    Field field;
                    try {
                        field = FluidSlotWidget.class.getDeclaredField("textRenderer");
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                    field.setAccessible(true);
                    try {
                        textRenderer = (TextRenderer) field.get(this);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                private void detectAndSendChangesAll() {
                    for (int ii = 0; ii < NECESSARY_MAX_SLOT; ii++) {
                        fluidSlotWidgets[ii].detectAndSendChanges(true);
                    }
                }
            };
            fluidSlotWidget.setBackground(getGUITextureSet().getItemSlot(), iDrawable);
            fluidSlotWidgets[i] = fluidSlotWidget;
            fluidSlotWidget.setPos(storedPos[i].add(3, 3));
            try {
                field.set(fluidSlotWidget, true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            builder.widget(fluidSlotWidget);
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

    protected void closeAllWindow(Widget widget) {
        if (widget.getContext().isClient()) {
            return;
        }
        for (int i : ALL_WINDOW_ID) {
            if (widget.getContext().isWindowOpen(i)) {
                widget.getContext().closeWindow(i);
            }
        }
        setAmount = null;
    }
}
