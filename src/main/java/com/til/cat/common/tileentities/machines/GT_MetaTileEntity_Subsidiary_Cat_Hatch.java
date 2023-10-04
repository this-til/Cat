package com.til.cat.common.tileentities.machines;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.render.TextureFactory;
import gregtech.common.tileentities.machines.GT_MetaTileEntity_Hatch_CraftingInput_ME;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_ME_CRAFTING_INPUT_SLAVE;

public class GT_MetaTileEntity_Subsidiary_Cat_Hatch extends GT_MetaTileEntity_Hatch implements IDualInputHatch {

    @Nullable
    protected GT_MetaTileEntity_Intelligence_Cat_Hatch master;
    protected int masterX, masterY, masterZ;

    protected boolean masterSet = true;

    public GT_MetaTileEntity_Subsidiary_Cat_Hatch(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            6,
            0,
            new String[]{
                "智能猫仓的从机",
                "使用闪存建立连接就像Crafting Input Buffer",
            }
        );
    }

    public GT_MetaTileEntity_Subsidiary_Cat_Hatch(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Subsidiary_Cat_Hatch(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return getTexturesInactive(aBaseTexture);
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, TextureFactory.of(OVERLAY_ME_CRAFTING_INPUT_SLAVE)};
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
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
        super.onPostTick(aBaseMetaTileEntity, aTimer);
        if (aTimer % 100 == 0 && masterSet && getMaster() == null) {
            trySetMasterFromCoord(masterX, masterY, masterZ);
        }
    }


    public GT_MetaTileEntity_Intelligence_Cat_Hatch getMaster() {
        if (master == null) return null;
        if (master.getBaseMetaTileEntity() == null) { // master disappeared
            master = null;
        }
        return master;
    }


    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        if (aNBT.hasKey("master")) {
            NBTTagCompound masterNBT = aNBT.getCompoundTag("master");
            masterX = masterNBT.getInteger("x");
            masterY = masterNBT.getInteger("y");
            masterZ = masterNBT.getInteger("z");
            masterSet = true;
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        if (masterSet) {
            NBTTagCompound masterNBT = new NBTTagCompound();
            masterNBT.setInteger("x", masterX);
            masterNBT.setInteger("y", masterY);
            masterNBT.setInteger("z", masterZ);
            aNBT.setTag("master", masterNBT);
        }
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    @Override
    public String[] getInfoData() {
        var ret = new ArrayList<String>();
        if (master != null) {
            ret.add(
                "连接智能猫仓在：" + masterX
                + ", "
                + masterY
                + ", "
                + masterZ
                + ".");
            ret.addAll(Arrays.asList(master.getInfoData()));
        } else ret.add("猫猫走丢了");
        return ret.toArray(new String[0]);
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
    public Iterator<IDualInputInventory> inventories() {
        return getMaster() != null ? getMaster().inventories() : Collections.emptyIterator();
    }

    @Override
    public boolean justUpdated() {
        return getMaster() != null && getMaster().justUpdated();
    }


    public GT_MetaTileEntity_Intelligence_Cat_Hatch trySetMasterFromCoord(int x, int y, int z) {
        var tileEntity = getBaseMetaTileEntity().getWorld()
            .getTileEntity(x, y, z);
        if (tileEntity == null) {
            return null;
        }
        if (!(tileEntity instanceof IGregTechTileEntity gtTileEntity)) {
            return null;
        }
        var metaTileEntity = gtTileEntity.getMetaTileEntity();
        if (!(metaTileEntity instanceof GT_MetaTileEntity_Intelligence_Cat_Hatch)) {
            return null;
        }
        masterX = x;
        masterY = y;
        masterZ = z;
        masterSet = true;
        master = (GT_MetaTileEntity_Intelligence_Cat_Hatch) metaTileEntity;
        return master;
    }


    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (!(aPlayer instanceof EntityPlayerMP)) {
            return false;
        }
        ItemStack dataStick = aPlayer.inventory.getCurrentItem();
        if (!ItemList.Tool_DataStick.isStackEqual(dataStick, true, true)) {
            return false;
        }
        if (!dataStick.hasTagCompound() || !"Intelligence Cat Hatch".equals(dataStick.stackTagCompound.getString("type"))) {
            return false;
        }

        NBTTagCompound nbt = dataStick.stackTagCompound;
        int x = nbt.getInteger("x");
        int y = nbt.getInteger("y");
        int z = nbt.getInteger("z");
        if (trySetMasterFromCoord(x, y, z) != null) {
            aPlayer.addChatMessage(new ChatComponentText("可以撸猫了，耶i"));
            return true;
        }
        aPlayer.addChatMessage(new ChatComponentText("猫猫成量子态了"));
        return false;
    }


}
