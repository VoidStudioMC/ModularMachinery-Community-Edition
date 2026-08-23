package net.voidstudio.mmce.common.tiles;

import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.minecraft.item.ItemStack;
import net.voidstudio.mmce.common.tiles.base.TileFluxEnergyHatch;
import sonar.fluxnetworks.api.network.ConnectionType;
import sonar.fluxnetworks.api.tiles.IFluxPoint;

import javax.annotation.Nullable;

public class TileFluxEnergyInputHatch extends TileFluxEnergyHatch implements IFluxPoint {

    public TileFluxEnergyInputHatch() {
        this.customName = "Flux Input Hatch";
        this.displayStack = new ItemStack(BlocksMM.fluxEnergyInputHatch);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.POINT;
    }

    @Override
    public long computeRequest() {
        long request = getRemainingCapacity();
        if (request <= 0) {
            return 0;
        }
        return Math.min(request, getLogicLimit());
    }

    @Override
    public boolean receiveEnergy(long receive) {
        return false;
    }

    @Nullable
    @Override
    public MachineComponent.EnergyHatch provideComponent() {
        return new MachineComponent.EnergyHatch(IOType.INPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileFluxEnergyInputHatch.this;
            }
        };
    }
}
