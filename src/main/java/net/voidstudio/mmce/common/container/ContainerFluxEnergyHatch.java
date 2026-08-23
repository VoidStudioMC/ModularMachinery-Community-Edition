package net.voidstudio.mmce.common.container;

import hellfirepvp.modularmachinery.common.container.ContainerEnergyHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.voidstudio.mmce.common.tiles.base.TileFluxEnergyHatch;
import sonar.fluxnetworks.api.network.INetworkConnector;

import javax.annotation.Nonnull;

public class ContainerFluxEnergyHatch extends ContainerEnergyHatch {
    protected INetworkConnector connector;

    public ContainerFluxEnergyHatch(TileFluxEnergyHatch owner, EntityPlayer opening) {
        super(owner, opening);
        this.connector = owner;
        this.connector.open(opening);
    }

    @Override
    public void onContainerClosed(@Nonnull EntityPlayer player) {
        super.onContainerClosed(player);
        connector.close(player);
    }
}
