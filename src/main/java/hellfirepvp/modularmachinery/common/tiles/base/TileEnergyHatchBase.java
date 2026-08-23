package hellfirepvp.modularmachinery.common.tiles.base;

import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.RedstoneHelper;
import mcjty.lib.api.power.IBigPower;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.voidstudio.mmce.common.item.ItemEnergyPowerModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

import static hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData.EMPTY;

@Optional.Interface(iface = "mcjty.lib.api.power.IBigPower", modid = "theoneprobe")
public abstract class TileEnergyHatchBase extends TileColorableMachineComponent implements
        IEnergyHandlerAsync,
        MachineComponentTile,
        SelectiveUpdateTileEntity,
        IBigPower {

    public static final int POWER_MODULE_SLOT = 0;
    protected final AtomicLong energy = new AtomicLong();
    protected EnergyHatchData size = EMPTY;
    private int prevRedstoneLevel = 0;

    protected IOInventory inventory;

    public TileEnergyHatchBase() {
        this.inventory = buildInventory();
        this.inventory.setStackLimit(1, POWER_MODULE_SLOT);
        this.inventory.setListener(this::onPowerModuleInventoryChanged);
    }

    @Override
    public long getCurrentEnergy() {
        return this.energy.get();
    }

    @Override
    public void setCurrentEnergy(long energy) {
        synchronized (this) {
            this.energy.set(MiscUtils.clamp(energy, 0, getMaxEnergy()));
        }
        markNoUpdateSync();
    }

    @Override
    public boolean extractEnergy(long extract) {
        boolean success = false;
        synchronized (this) {
            if (this.energy.get() >= extract) {
                this.energy.addAndGet(-extract);
                success = true;
            }
        }
        if (success) {
            markNoUpdateSync();
        }
        return success;
    }

    @Override
    public boolean receiveEnergy(long receive) {
        boolean success = false;
        synchronized (this) {
            if (getRemainingCapacity() >= receive) {
                this.energy.addAndGet(receive);
                success = true;
            }
        }
        if (success) {
            markNoUpdateSync();
        }
        return success;
    }

    @Override
    public long getMaxEnergy() {
        return this.size.maxEnergy;
    }

    @Override
    public long getStoredPower() {
        return energy.get();
    }

    @Override
    public long getCapacity() {
        return size.maxEnergy;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }

        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.inventory;
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        NBTBase energyTag = compound.getTag("energy");
        if (energyTag instanceof NBTPrimitive) {
            this.energy.set(((NBTPrimitive) energyTag).getLong());
        }
        this.size = EnergyHatchData.values()[compound.getInteger("hatchSize")];

        this.inventory = IOInventory.deserialize(this, compound.getCompoundTag("items"));
        this.inventory.setStackLimit(1, POWER_MODULE_SLOT);
        this.inventory.setListener(this::onPowerModuleInventoryChanged);

        onPowerModuleInventoryChanged(-1);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setLong("energy", this.energy.get());
        compound.setInteger("hatchSize", this.size.ordinal());
        compound.setTag("items", this.inventory.writeNBT());
    }

    @Override
    public void markNoUpdate() {
        int redstoneLevel = RedstoneHelper.getRedstoneLevel(this);
        if (prevRedstoneLevel != redstoneLevel) {
            prevRedstoneLevel = redstoneLevel;
            this.requireUpdateComparatorLevel = true;
        }
        super.markNoUpdate();
        this.requireUpdateComparatorLevel = false;
    }

    public EnergyHatchData getTier() {
        return size;
    }

    public IOInventory getInventory() {
        return inventory;
    }

    protected IOInventory buildInventory() {
        return (IOInventory) new IOInventory(this, new int[0], new int[0]).setMiscSlots(POWER_MODULE_SLOT);
    }

    public synchronized void onPowerModuleInventoryChanged(int changedSlot) {
        var stack = this.inventory.getStackInSlot(POWER_MODULE_SLOT);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemEnergyPowerModule) {
            this.size = EnergyHatchData.values()[stack.getMetadata()];
        } else {
            this.size = EMPTY;
        }
    }
}
