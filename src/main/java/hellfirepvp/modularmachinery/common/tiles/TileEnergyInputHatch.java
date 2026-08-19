/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyStorageCore;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.integration.IntegrationIC2EventHandlerHelper;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;

import static hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData.enableDEIntegration;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEnergyInputHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 12:47
 */
@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "ic2")
public class TileEnergyInputHatch extends TileEnergyHatch implements IEnergySink {

    public TileEnergyInputHatch() {
        super(IOType.INPUT);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }

        if (!tickedOnce) {
            if (Mods.IC2.isPresent()) {
                IntegrationIC2EventHandlerHelper.onEnergyTileLoaded(this);
            }
            tickedOnce = true;
        }

        long maxCanReceive = Math.min(this.size.transferLimit, this.size.maxEnergy - this.energy.get());
        if (maxCanReceive <= 0) {
            return;
        }

        if (Mods.DRACONICEVOLUTION.isPresent() && enableDEIntegration) {
            long received = attemptDECoreTransfer(maxCanReceive);
            if (received != 0) {
                this.energy.addAndGet(received);
                markNoUpdateSync();
            }
        }
    }

    @Optional.Method(modid = "draconicevolution")
    protected long attemptDECoreTransfer(long maxCanReceive) {
        TileEntity te = foundCore == null ? null : world.getTileEntity(foundCore);
        if (foundCore == null || !(te instanceof final TileEnergyStorageCore core)) {
            foundCore = null;
            findCore();
            return 0;
        }

        long received = Math.min(core.energy.value, maxCanReceive);
        core.energy.value -= received;
        return received;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public void invalidate() {
        super.invalidate();
        if (Mods.IC2.isPresent() && tickedOnce && !world.isRemote) {
            IntegrationIC2EventHandlerHelper.onEnergyTileUnLoaded(this);
        }
    }

    @Override
    @Optional.Method(modid = "ic2")
    public void onChunkUnload() {
        super.onChunkUnload();
        if (Mods.IC2.isPresent() && tickedOnce && !world.isRemote) {
            IntegrationIC2EventHandlerHelper.onEnergyTileUnLoaded(this);
        }
    }

    @Override
    @Optional.Method(modid = "ic2")
    public double getDemandedEnergy() {
        return Math.min((this.size.maxEnergy - this.energy.get()) / 4, this.size.getIC2EnergyTransmission());
    }

    @Override
    @Optional.Method(modid = "ic2")
    public int getSinkTier() {
        return this.size.ic2EnergyTier;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        long addable = Math.min((this.size.maxEnergy - this.energy.get()) / 4L, MathHelper.lfloor(amount));
        amount -= addable;
        this.energy.set(MiscUtils.clamp(this.energy.get() + MathHelper.lfloor(addable * 4), 0, this.size.maxEnergy));
        markNoUpdateSync();
        return amount;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing side) {
        return true;
    }

    @Nullable
    @Override
    public MachineComponent.EnergyHatch provideComponent() {
        return new MachineComponent.EnergyHatch(IOType.INPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileEnergyInputHatch.this;
            }
        };
    }
}
