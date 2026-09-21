/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles.base;

import com.brandon3055.draconicevolution.DEFeatures;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyStorageCore;
import gregtech.api.capability.GregtechCapabilities;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import mcjty.lib.api.power.IBigPower;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData.*;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEnergyHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 10:14
 */
@Optional.Interface(iface = "cofh.redstoneflux.api.IEnergyStorage", modid = "redstoneflux")
public abstract class TileEnergyHatch extends TileEnergyHatchBase implements
        ITickable,
        IEnergyStorage,
        cofh.redstoneflux.api.IEnergyStorage,
        IBigPower {

    protected BlockPos foundCore = null;
    protected int energyCoreSearchFailedCount = 0;
    private GTEnergyContainer energyContainer;

    protected boolean tickedOnce = false;

    public TileEnergyHatch() {
        super();
    }

    public TileEnergyHatch(IOType ioType) {
        this();
        this.energyContainer = new GTEnergyContainer(this, ioType);
    }

    @Optional.Method(modid = "draconicevolution")
    protected long attemptDECoreTransfer(long maxCanReceive) {
        return 0;
    }

    @Optional.Method(modid = "draconicevolution")
    protected void findCore() {
        if (!(world.getTotalWorldTime() % currentFoundCoreDelay() == 0)) {
            return;
        }

        TileEnergyStorageCore core = null;
        Iterable<BlockPos.MutableBlockPos> positions = BlockPos.getAllInBoxMutable(pos.add(-searchRange, -searchRange, -searchRange), pos.add(searchRange, searchRange, searchRange));

        // The cursor is used only for block-state reads. Preserve the original x/y/z order
        // and pass an immutable position to tile lookup, where code may retain it.
        for (BlockPos blockPos : positions) {
            if (world.getBlockState(blockPos).getBlock() == DEFeatures.energyStorageCore) {
                TileEntity tile = world.getTileEntity(blockPos.toImmutable());
                if (tile instanceof TileEnergyStorageCore && ((TileEnergyStorageCore) tile).active.value) {
                    core = (TileEnergyStorageCore) tile;
                    break;
                }
            }
        }

        if (core == null) {
            energyCoreSearchFailedCount++;
        } else {
            foundCore = core.getPos();
            energyCoreSearchFailedCount = 0;
        }
    }

    protected int currentFoundCoreDelay() {
        return energyCoreSearchDelay + (delayedEnergyCoreSearch
                ? (Math.min(energyCoreSearchFailedCount * 20, maxEnergyCoreSearchDelay - energyCoreSearchDelay))
                : 0);
    }

    @Optional.Method(modid = "gregtech")
    private static Capability<?> getGTEnergyCapability() {
        return GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
    }

    protected static int convertDownEnergy(long energy) {
        return energy >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) energy;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }
        int insertable = this.energy.get() + maxReceive > this.size.maxEnergy ? convertDownEnergy(this.size.maxEnergy - this.energy.get()) : maxReceive;
        insertable = Math.min(insertable, convertDownEnergy(size.transferLimit));
        if (!simulate) {
            this.energy.set(MiscUtils.clamp(this.energy.get() + insertable, 0, this.size.maxEnergy));
            markNoUpdate();
        }
        return insertable;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) {
            return 0;
        }
        int extractable = this.energy.get() - maxExtract < 0 ? convertDownEnergy(this.energy.get()) : maxExtract;
        extractable = Math.min(extractable, convertDownEnergy(size.transferLimit));
        if (!simulate) {
            this.energy.set(MiscUtils.clamp(this.energy.get() - extractable, 0, this.size.maxEnergy));
            markNoUpdate();
        }
        return extractable;
    }

    @Override
    public int getEnergyStored() {
        return convertDownEnergy(this.energy.get());
    }

    @Override
    public int getMaxEnergyStored() {
        return convertDownEnergy(this.size.maxEnergy);
    }

    @Override
    public abstract boolean canExtract();

    @Override
    public abstract boolean canReceive();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }

        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) this;
        }
        if (Mods.GREGTECH.isPresent() && capability == getGTEnergyCapability()) {
            return (T) this.energyContainer;
        }

        return super.getCapability(capability, facing);
    }
}
