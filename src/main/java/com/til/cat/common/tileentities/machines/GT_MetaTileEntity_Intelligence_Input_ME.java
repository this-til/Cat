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
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.google.common.collect.ImmutableList;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.forge.ItemStackHandler;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.Color;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;
import com.gtnewhorizons.modularui.common.widget.*;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;
import com.til.cat.common.crafting_type.CraftingType;
import com.til.cat.common.loaders.Cat_Loader_MetaTileEntities;
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
import gregtech.common.gui.modularui.widget.CoverCycleButtonWidget;
import gregtech.common.tileentities.machines.GT_MetaTileEntity_Hatch_CraftingInput_ME;
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
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_ME_CRAFTING_INPUT_BUFFER;

/***
 * 智能输入仓
 */
public class GT_MetaTileEntity_Intelligence_Input_ME
    extends GT_MetaTileEntity_Hatch
    implements IDualInputHatch, IPowerChannelState, ICraftingProvider,
    IGridProxyable, IAddUIWidgets, IDualInputInventory {

    public static final int NECESSARY_MAX_SLOT = 9;
    protected static final int INPUT_NECESSARY_ITEM_WINDOW_ID = 10;
    protected static final int OUT_NECESSARY_ITEM_WINDOW_ID = 11;
    protected static final int INPUT_NECESSARY_FLUID_WINDOW_ID = 12;
    protected static final int OUT_NECESSARY_FLUID_WINDOW_ID = 13;
    protected static final int CONFIGURATION_MULTIPLE = 14;

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

    protected ItemStackHandler inputNecessaryItemHandler = new ItemStackHandler(inputNecessaryItem);

    protected ItemStackHandler outNecessaryItemItemHandle = new ItemStackHandler(outNecessaryItem);

    protected FluidStackTank[] inputNecessaryFluidTanks = new FluidStackTank[NECESSARY_MAX_SLOT];
    protected FluidStackTank[] outNecessaryFluidTanks = new FluidStackTank[NECESSARY_MAX_SLOT];


    protected List<ItemStack> itemInventory = new ArrayList<>();
    protected List<FluidStack> fluidInventory = new ArrayList<>();

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

    protected List<ICraftingPatternDetails> craftingPatternDetailsList = new ArrayList<>();

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
        for (int i = 0; i < NECESSARY_MAX_SLOT; i++) {
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
        aNBT.setBoolean("excludeProbability", excludeProbability);
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
        excludeProbability = aNBT.getBoolean("excludeProbability");

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
        craftingPatternDetailsList = craftingType.provideCrafting(this, craftingTracker);

        for (ICraftingPatternDetails iCraftingPatternDetails : craftingPatternDetailsList) {
            craftingTracker.addCraftingOption(this, iCraftingPatternDetails);
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
        tooltip.add("机械类型:" + StatCollector.translateToLocal(craftingType.getGtRecipeMap().mUnlocalizedName));
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        NBTTagCompound tag = accessor.getNBTData();
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
        buildContext.addSyncedWindow(INPUT_NECESSARY_ITEM_WINDOW_ID, player -> necessaryItemConfigurationWindow(player, inputNecessaryItem, inputNecessaryItemHandler));
        buildContext.addSyncedWindow(OUT_NECESSARY_ITEM_WINDOW_ID, player -> necessaryItemConfigurationWindow(player, outNecessaryItem, outNecessaryItemItemHandle));
        buildContext.addSyncedWindow(INPUT_NECESSARY_FLUID_WINDOW_ID, player -> necessaryFluidConfigurationWindow(player, inputNecessaryFluid, inputNecessaryFluidTanks));
        buildContext.addSyncedWindow(OUT_NECESSARY_FLUID_WINDOW_ID, player -> necessaryFluidConfigurationWindow(player, outNecessaryFluid, outNecessaryFluidTanks));
        buildContext.addSyncedWindow(CONFIGURATION_MULTIPLE, this::createConfigurationMultipleWindow);

        {
            ButtonWidget itemInButtonWidget = new ButtonWidget();

            itemInButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    widget.getContext().openSyncedWindow(INPUT_NECESSARY_ITEM_WINDOW_ID);
                }
            });
            itemInButtonWidget.setPlayClickSound(true);
            itemInButtonWidget.setBackground(GT_UITextures.BUTTON_STANDARD, GT_UITextures.PICTURE_ITEM_IN);
            itemInButtonWidget.addTooltips(ImmutableList.of("打开输入物品限制配置窗口"));
            itemInButtonWidget.setSize(16, 16);
            itemInButtonWidget.setPos(7 + 18 * 0, 9);

            builder.widget(itemInButtonWidget);
        }
        {
            ButtonWidget itemOutButtonWidget = new ButtonWidget();

            itemOutButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    widget.getContext().openSyncedWindow(OUT_NECESSARY_ITEM_WINDOW_ID);
                }
            });
            itemOutButtonWidget.setPlayClickSound(true);
            itemOutButtonWidget.setBackground(GT_UITextures.BUTTON_STANDARD, GT_UITextures.PICTURE_ITEM_OUT);
            itemOutButtonWidget.addTooltips(ImmutableList.of("打开输出物品限制配置窗口"));
            itemOutButtonWidget.setSize(16, 16);
            itemOutButtonWidget.setPos(7 + 18 * 1, 9);
            builder.widget(itemOutButtonWidget);
        }

        {


            ButtonWidget fluidInButtonWidget = new ButtonWidget();

            fluidInButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    widget.getContext().openSyncedWindow(INPUT_NECESSARY_FLUID_WINDOW_ID);
                }
            });
            fluidInButtonWidget.setPlayClickSound(true);
            fluidInButtonWidget.setBackground(GT_UITextures.BUTTON_STANDARD, GT_UITextures.PICTURE_FLUID_IN);
            fluidInButtonWidget.addTooltips(ImmutableList.of("打开输入流体限制配置窗口"));
            fluidInButtonWidget.setSize(16, 16);
            fluidInButtonWidget.setPos(7 + 18 * 2, 9);

            builder.widget(fluidInButtonWidget);
        }

        {

            ButtonWidget fluidOutButtonWidget = new ButtonWidget();

            fluidOutButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    widget.getContext().openSyncedWindow(OUT_NECESSARY_FLUID_WINDOW_ID);
                }
            });
            fluidOutButtonWidget.setPlayClickSound(true);
            fluidOutButtonWidget.setBackground(GT_UITextures.BUTTON_STANDARD, GT_UITextures.PICTURE_FLUID_OUT);
            fluidOutButtonWidget.addTooltips(ImmutableList.of("打开输出流体限制配置窗口"));
            fluidOutButtonWidget.setSize(16, 16);
            fluidOutButtonWidget.setPos(7 + 18 * 3, 9);

            builder.widget(fluidOutButtonWidget);
        }

        {


            ButtonWidget multipleButtonWidget = new ButtonWidget();
            multipleButtonWidget.setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    widget.getContext().openSyncedWindow(CONFIGURATION_MULTIPLE);
                }
            });
            multipleButtonWidget.setPlayClickSound(true);
            multipleButtonWidget.setBackground(GT_UITextures.BUTTON_STANDARD, GT_UITextures.OVERLAY_BUTTON_DOWN_TIERING_OFF);
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
            exportButtonWidget.setPos(7 + 18 * 5, 9);


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
                    .setNumbers(1, Integer.MAX_VALUE)
                    .setOnScrollNumbers(1, 1, 65536)
                    .setTextAlignment(Alignment.Center)
                    .setTextColor(Color.WHITE.normal)
                    .setSize(36, 18)
                    .setPos(19, 18)
                    .setBackground(GT_UITextures.BACKGROUND_TEXT_FIELD));
        return builder.build();
    }


    protected ModularWindow necessaryItemConfigurationWindow(EntityPlayer player, ItemStack[] necessaryItemStack, ItemStackHandler inputNecessaryItemHandler) {

        ModularWindow.Builder builder = ModularWindow.builder(getGUIWidth(), getGUIHeight());
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);
        SlotWidget[] slotWidgets = new SlotWidget[NECESSARY_MAX_SLOT];
        SlotGroup.ItemGroupBuilder itemGroupBuilder = SlotGroup.ofItemHandler(inputNecessaryItemHandler, 3);
        itemGroupBuilder.startFromSlot(0);
        itemGroupBuilder.endAtSlot(8);
        itemGroupBuilder.phantom(true);
        itemGroupBuilder.background(getGUITextureSet().getItemSlot(), GT_UITextures.OVERLAY_SLOT_ARROW_ME);
        itemGroupBuilder.widgetCreator(slot -> {
            SlotWidget slotWidget = new SlotWidget(slot) {
                @Override
                protected void phantomClick(ClickData clickData, ItemStack cursorStack) {
                    if (clickData.mouseButton != 0) {
                        return;
                    }
                    if (cursorStack == null) {
                        if (necessaryItemStack[slot.getSlotIndex()] == null) {
                            return;
                        }
                        necessaryItemStack[slot.getSlotIndex()] = null;
                        slotWidgets[slot.getSlotIndex()].getMcSlot().putStack(null);
                        slotWidgets[slot.getSlotIndex()].detectAndSendChanges(true);
                        needPatternSync = true;
                        for (int i = slot.getSlotIndex(); i < NECESSARY_MAX_SLOT - 1; i++) {
                            if (necessaryItemStack[i + 1] == null) {
                                break;
                            }
                            necessaryItemStack[i] = necessaryItemStack[i + 1];
                            slotWidgets[i].getMcSlot().putStack(necessaryItemStack[i + 1]);
                            if (i != slot.getSlotIndex()) {
                                slotWidgets[i].detectAndSendChanges(true);
                            }
                        }
                        if (necessaryItemStack[NECESSARY_MAX_SLOT - 1] != null) {
                            necessaryItemStack[NECESSARY_MAX_SLOT - 1] = null;
                            slotWidgets[NECESSARY_MAX_SLOT - 1].getMcSlot().putStack(null);
                            slotWidgets[NECESSARY_MAX_SLOT - 1].detectAndSendChanges(true);
                        }
                        return;
                    }
                    ItemStack addItemStack = GT_Utility.copyAmount(1L, cursorStack);

                    for (int i = 0; i < NECESSARY_MAX_SLOT; i++) {
                        if (necessaryItemStack[i] == null) {
                            necessaryItemStack[i] = addItemStack;
                            slotWidgets[i].getMcSlot().putStack(addItemStack);
                            needPatternSync = true;
                            slotWidgets[i].detectAndSendChanges(true);
                            break;
                        }
                        if (GT_Utility.areStacksEqual(necessaryItemStack[i], addItemStack)) {
                            break;
                        }
                    }
                }
            };
            slotWidget.getMcSlot().putStack(inputNecessaryItem[slot.getSlotIndex()]);
            slotWidgets[slot.getSlotIndex()] = slotWidget;
            return slotWidget;
        });
        builder.widget(itemGroupBuilder.build().setPos(7, 9));
        return builder.build();
    }

    protected ModularWindow necessaryFluidConfigurationWindow(EntityPlayer player, FluidStack[] necessaryFluidStack, FluidStackTank[] fluidStackTanks) {
        ModularWindow.Builder builder = ModularWindow.builder(getGUIWidth(), getGUIHeight());
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.setGuiTint(getGUIColorization());
        builder.setDraggable(true);
        FluidSlotWidget[] fluidSlotWidgets = new FluidSlotWidget[NECESSARY_MAX_SLOT];
        for (int i = 0; i < NECESSARY_MAX_SLOT; i++) {
            FluidStackTank fluidTank = fluidStackTanks[i];
            int finalI = i;
            FluidSlotWidget fluidSlotWidget = new FluidSlotWidget(fluidTank) {
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
                            if (clickData.mouseButton != 0) {
                                return;
                            }
                            clearTag();
                            break;
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
                        fluidSlotWidgets[i].detectAndSendChanges(true);
                        if (necessaryFluidStack[i + 1] == null) {
                            break;
                        }
                        necessaryFluidStack[i] = necessaryFluidStack[i + 1];
                    }
                    if (necessaryFluidStack[NECESSARY_MAX_SLOT - 1] != null) {
                        necessaryFluidStack[NECESSARY_MAX_SLOT - 1] = null;
                        fluidSlotWidgets[NECESSARY_MAX_SLOT - 1].detectAndSendChanges(true);
                    }
                }

                @Override
                protected void onClickServer(ClickData clickData, ItemStack clientVerifyToken) {
                    EntityPlayer player = getContext().getPlayer();
                    ItemStack heldItem = player.inventory.getItemStack();
                    if (clickData.mouseButton != 0) {
                        return;
                    }
                    if (heldItem == null) {
                        //needPatternSync = needPatternSync || necessaryFluidStack[finalI] != null;
                        //necessaryFluidStack[finalI] = null;
                        //detectAndSendChanges(false);
                        clearTag();
                        return;
                    }
                    FluidStack heldFluid = getFluidForPhantomItem(heldItem);
                    if (heldFluid == null) {
                        return;
                    }
                    FluidStack setFileStack = GT_Utility.copyAmount(1, heldFluid);
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
            };
            fluidSlotWidget.setBackground(getGUITextureSet().getItemSlot(), GT_UITextures.OVERLAY_SLOT_ARROW_ME);
            fluidSlotWidgets[i] = fluidSlotWidget;
            fluidSlotWidget.setPos(storedPos[i].add(7, 9));
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

}
