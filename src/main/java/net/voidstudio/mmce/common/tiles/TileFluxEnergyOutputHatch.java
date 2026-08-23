package net.voidstudio.mmce.common.tiles;

import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.minecraft.item.ItemStack;
import net.voidstudio.mmce.common.tiles.base.TileFluxEnergyHatch;
import sonar.fluxnetworks.api.network.ConnectionType;
import sonar.fluxnetworks.api.tiles.IFluxPlug;

import javax.annotation.Nullable;

public class TileFluxEnergyOutputHatch extends TileFluxEnergyHatch implements IFluxPlug {

    public TileFluxEnergyOutputHatch() {
        this.customName = "Flux Output Hatch";
        this.displayStack = new ItemStack(BlocksMM.fluxEnergyOutputHatch);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.PLUG;
    }

    @Override
    public long computeRequest() {
        return 0;
    }

    @Override
    public boolean extractEnergy(long extract) {
        return false;
    }

    @Nullable
    @Override
    public MachineComponent.EnergyHatch provideComponent() {
        return new MachineComponent.EnergyHatch(IOType.OUTPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileFluxEnergyOutputHatch.this;
            }
        };
    }
}
