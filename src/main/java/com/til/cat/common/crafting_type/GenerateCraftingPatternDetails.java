package com.til.cat.common.crafting_type;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.util.item.AEItemStack;
import com.glodblock.github.loader.ItemAndBlockHolder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

public class GenerateCraftingPatternDetails implements ICraftingPatternDetails, Comparable<GenerateCraftingPatternDetails> {

    protected final ItemStack pattern;

    protected final IAEItemStack patternStackAe;
    protected final IAEItemStack[] inputs;
    protected final IAEItemStack[] outputs;

    protected final boolean canSubstitute;
    protected final boolean canBeSubstitute;

    protected final boolean hasFluid;

    protected int priority;


    public GenerateCraftingPatternDetails(IAEItemStack[] inputs, IAEItemStack[] outputs, boolean substitute, boolean beSubstitute, boolean hasFluid) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.canSubstitute = substitute;
        this.canBeSubstitute = beSubstitute;
        this.hasFluid = hasFluid;
        this.pattern = new ItemStack(hasFluid ? ItemAndBlockHolder.PATTERN: Api.INSTANCE.definitions().items().encodedPattern().maybeItem().get());
        writeToStack();
        this.patternStackAe = AEItemStack.create(pattern);
    }


    @Override
    public ItemStack getPattern() {
        //TODO
        return pattern;
    }

    @Override
    public boolean isValidItemForSlot(int slotIndex, ItemStack itemStack, World world) {
        throw new IllegalStateException("Only crafting recipes supported.");
    }

    @Override
    public boolean isCraftable() {
        return false;
    }

    @Override
    public IAEItemStack[] getInputs() {
        return inputs;
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return inputs;
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return outputs;
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return outputs;
    }

    @Override
    public boolean canSubstitute() {
        return canSubstitute;
    }

    @Override
    public boolean canBeSubstitute() {
        return canBeSubstitute;
    }

    @Override
    public ItemStack getOutput(InventoryCrafting craftingInv, World world) {
        throw new IllegalStateException("Only crafting recipes supported.");
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int hashCode() {
        return patternStackAe.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GenerateCraftingPatternDetails && patternStackAe.equals(((GenerateCraftingPatternDetails) obj).patternStackAe);
    }

    @Override
    public int compareTo(GenerateCraftingPatternDetails o) {
        return Integer.compare(o.priority, this.priority);
    }

    public void writeToStack() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("in", writeStackArray(inputs));
        tag.setTag("out", writeStackArray(outputs));
        tag.setBoolean("beSubstitute", this.canBeSubstitute());
        pattern.setTagCompound(tag);
    }

    public static NBTTagList writeStackArray(IAEItemStack[] stacks) {
        NBTTagList listTag = new NBTTagList();
        for (IAEItemStack stack : stacks) {
            // see note at top of class
            NBTTagCompound stackTag = new NBTTagCompound();
            if (stack != null) stack.writeToNBT(stackTag);
            listTag.appendTag(stackTag);
        }
        return listTag;
    }
}