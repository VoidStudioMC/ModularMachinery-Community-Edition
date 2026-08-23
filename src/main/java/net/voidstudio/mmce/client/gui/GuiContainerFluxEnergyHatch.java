package net.voidstudio.mmce.client.gui;

import hellfirepvp.modularmachinery.client.gui.GuiContainerEnergyHatch;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.voidstudio.mmce.client.gui.button.SettingsButtonEnergyHatch;
import net.voidstudio.mmce.common.container.ContainerFluxEnergyHatch;
import net.voidstudio.mmce.common.tiles.base.TileFluxEnergyHatch;
import sonar.fluxnetworks.api.tiles.IFluxConnector;
import sonar.fluxnetworks.client.gui.GuiFluxConnectorHome;

import javax.annotation.Nonnull;
import java.io.IOException;

public class GuiContainerFluxEnergyHatch extends GuiContainerEnergyHatch {
    private final EntityPlayer player;

    public GuiContainerFluxEnergyHatch(TileFluxEnergyHatch fluxEnergyHatch, EntityPlayer opening) {
        super(new ContainerFluxEnergyHatch(fluxEnergyHatch, opening));
        this.player = opening;
    }

    @Override
    public void initGui() {
        super.initGui();

        addButton(new SettingsButtonEnergyHatch(100,guiLeft + 9,guiTop + 54));
    }

    @Override
    protected void actionPerformed(@Nonnull GuiButton button) throws IOException {
        super.actionPerformed(button);

        if (button.id == 100 && energyHatch instanceof IFluxConnector connector) {
            FMLCommonHandler.instance().showGuiScreen(new GuiFluxConnectorHome(player, connector));
        }
    }
}
