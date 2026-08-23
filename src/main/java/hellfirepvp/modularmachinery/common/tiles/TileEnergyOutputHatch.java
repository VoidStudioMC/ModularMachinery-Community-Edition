/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import cofh.redstoneflux.api.IEnergyConnection;
import cofh.redstoneflux.api.IEnergyReceiver;
import cofh.redstoneflux.api.IEnergyStorage;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyStorageCore;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.integration.IntegrationIC2EventHandlerHelper;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.common.Optional;
import sonar.fluxnetworks.common.tileentity.TileFluxPlug;

import javax.annotation.Nullable;

import static hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData.enableDEIntegration;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEnergyOutputHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 12:43
 */
@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "ic2")
public class TileEnergyOutputHatch extends TileEnergyHatch implements IEnergySource {
    protected long maxCanExtractTick;

    public TileEnergyOutputHatch() {
        super(IOType.OUTPUT);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
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

        long prevEnergy = this.energy.get();
        maxCanExtractTick = Math.min(this.size.transferLimit, this.energy.get());
        if (maxCanExtractTick <= 0) {
            return;
        }

        // DE Transfer
        if (Mods.DRACONICEVOLUTION.isPresent() && enableDEIntegration) {
            long transferred = attemptDECoreTransfer(maxCanExtractTick);
            maxCanExtractTick -= transferred;
            this.energy.addAndGet(-transferred);
        }

        long usableAmps = Mods.GREGTECH.isPresent() ? Math.min(this.size.getGtAmperage(), maxCanExtractTick / 4L / this.size.getGTEnergyTransferVoltage()) : 0;
        for (EnumFacing face : EnumFacing.VALUES) {
            // FluxNetworks Transfer
            if (maxCanExtractTick > 0 && Mods.FLUX_NETWORKS.isPresent() && Config.enableFluxNetworksIntegration) {
                long transferred = attemptFluxNetworksTransfer(face, maxCanExtractTick);
                this.energy.addAndGet(-transferred);
                maxCanExtractTick -= transferred;
            }
            // GT Transfer
            if (maxCanExtractTick > 0 && Mods.GREGTECH.isPresent() && usableAmps > 0) {
                long totalTransferred = attemptGTTransfer(face, maxCanExtractTick / 4L, usableAmps) * 4L;
                usableAmps -= totalTransferred / 4L / this.size.getGTEnergyTransferVoltage();
                maxCanExtractTick -= totalTransferred;
                this.energy.addAndGet(-totalTransferred);
            }
            // FE / RF Transfer
            if (maxCanExtractTick > 0) {
                int transferred;

                if (Mods.REDSTONEFLUXAPI.isPresent()) {
                    transferred = attemptFERFTransfer(face, convertDownEnergy(maxCanExtractTick));
                } else {
                    transferred = attemptFETransfer(face, convertDownEnergy(maxCanExtractTick));
                }
                maxCanExtractTick -= transferred;
                this.energy.addAndGet(-transferred);
            }
            if (maxCanExtractTick <= 0) {
                break;
            }
        }

        if (prevEnergy != this.energy.get()) {
            markNoUpdateSync();
        }
    }

    @Optional.Method(modid = "fluxnetworks")
    protected long attemptFluxNetworksTransfer(EnumFacing face, long maxCanExtract) {
        BlockPos at = this.getPos().offset(face);
        EnumFacing oppositeSide = face.getOpposite();

        TileEntity te = world.getTileEntity(at);
        if (te instanceof final TileFluxPlug plug) {
            long maxCanReceive = Math.min(plug.getMaxTransferLimit() - plug.getTransferBuffer(), maxCanExtract);
            return plug.getTransferHandler().receiveFromSupplier(maxCanReceive, oppositeSide, false);
        }

        return 0;
    }

    @Optional.Method(modid = "draconicevolution")
    protected long attemptDECoreTransfer(long maxCanExtract) {
        TileEntity te = foundCore == null ? null : world.getTileEntity(foundCore);
        if (foundCore == null || !(te instanceof TileEnergyStorageCore)) {
            foundCore = null;
            findCore();
        }

        if (foundCore != null && te instanceof final TileEnergyStorageCore core) {

            long energyReceived = Math.min(core.getExtendedCapacity() - core.energy.value, maxCanExtract);
            core.energy.value += energyReceived;

            return energyReceived;
        }
        return 0;
    }

    @Optional.Method(modid = "gregtech")
    private long attemptGTTransfer(EnumFacing face, long transferCap, long usedAmps) {
        long voltage = this.size.getGTEnergyTransferVoltage();
        long amperes = Math.min(usedAmps, this.size.getGtAmperage());
        int transferableAmps = 0;
        while (transferableAmps < amperes && (transferableAmps * voltage) <= transferCap) {
            transferableAmps++;
        }
        if (transferableAmps == 0) {
            return 0L;
        }

        TileEntity tileEntity = getWorld().getTileEntity(getPos().offset(face));
        EnumFacing oppositeSide = face.getOpposite();
        if (tileEntity != null && tileEntity.hasCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, oppositeSide)) {
            IEnergyContainer energyContainer = tileEntity.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, oppositeSide);
            if (energyContainer != null && energyContainer.inputsEnergy(oppositeSide)) {
                return energyContainer.acceptEnergyFromNetwork(oppositeSide, voltage, transferableAmps) * voltage;
            }
        }
        return 0L;
    }

    private int attemptFETransfer(EnumFacing face, int maxTransferLeft) {
        BlockPos at = this.getPos().offset(face);
        EnumFacing accessingSide = face.getOpposite();

        int receivedEnergy = 0;
        TileEntity te = world.getTileEntity(at);
        if (te != null && !(te instanceof TileEnergyHatch)) {
            if (te.hasCapability(CapabilityEnergy.ENERGY, accessingSide)) {
                net.minecraftforge.energy.IEnergyStorage ce = te.getCapability(CapabilityEnergy.ENERGY, accessingSide);
                if (ce != null && ce.canReceive()) {
                    receivedEnergy = ce.receiveEnergy(maxTransferLeft, false);
                }
            }
        }
        return receivedEnergy;
    }

    @Optional.Method(modid = "redstoneflux")
    private int attemptFERFTransfer(EnumFacing face, int maxTransferLeft) {
        BlockPos at = this.getPos().offset(face);
        EnumFacing accessingSide = face.getOpposite();

        int receivedEnergy = 0;
        TileEntity te = world.getTileEntity(at);
        if (te != null && !(te instanceof TileEnergyHatch)) {
            if (te instanceof cofh.redstoneflux.api.IEnergyReceiver && ((IEnergyConnection) te).canConnectEnergy(accessingSide)) {
                try {
                    receivedEnergy = ((IEnergyReceiver) te).receiveEnergy(accessingSide, maxTransferLeft, false);
                } catch (Exception ignored) {
                }
            }
            if (receivedEnergy <= 0 && te instanceof IEnergyStorage) {
                try {
                    receivedEnergy = ((IEnergyStorage) te).receiveEnergy(maxTransferLeft, false);
                } catch (Exception ignored) {
                }
            }
            if (receivedEnergy <= 0 && te.hasCapability(CapabilityEnergy.ENERGY, accessingSide)) {
                net.minecraftforge.energy.IEnergyStorage ce = te.getCapability(CapabilityEnergy.ENERGY, accessingSide);
                if (ce != null && ce.canReceive()) {
                    try {
                        receivedEnergy = ce.receiveEnergy(maxTransferLeft, false);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return receivedEnergy;
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
    public double getOfferedEnergy() {
        return Math.min(this.size.getIC2EnergyTransmission(),
                Math.min(this.getCurrentEnergy() / 4L, maxCanExtractTick / 4L));
    }

    @Override
    @Optional.Method(modid = "ic2")
    public void drawEnergy(double amount) {
        long energy = MathHelper.lfloor(amount) * 4L;
        this.energy.set(MiscUtils.clamp(this.energy.get() - energy, 0, this.size.maxEnergy));
        maxCanExtractTick -= energy;
        markNoUpdateSync();
    }

    @Override
    @Optional.Method(modid = "ic2")
    public int getSourceTier() {
        return size.ic2EnergyTier;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side) {
        return true;
    }

    @Nullable
    @Override
    public MachineComponent.EnergyHatch provideComponent() {
        return new MachineComponent.EnergyHatch(IOType.OUTPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileEnergyOutputHatch.this;
            }
        };
    }
}
