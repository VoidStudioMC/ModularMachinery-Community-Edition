/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.gui;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.container.ContainerEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Optional;

import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: GuiContainerEnergyHatch
 * Created by HellFirePvP
 * Date: 09.07.2017 / 14:26
 */
public class GuiContainerEnergyHatch extends GuiContainerBase<ContainerEnergyHatch> {

    public static final ResourceLocation TEXTURES_ENERGY_HATCH = new ResourceLocation(ModularMachinery.MODID, "textures/gui/energyhatch.png");

    private final TileEnergyHatch energyHatch;

    public GuiContainerEnergyHatch(TileEnergyHatch tileFluidTank, EntityPlayer opening) {
        super(new ContainerEnergyHatch(tileFluidTank, opening));
        this.energyHatch = tileFluidTank;
    }

    @Override
    protected void setWidthHeight() {
    }

    @Override
    protected void renderHoveredToolTip(int x, int z) {
        super.renderHoveredToolTip(x, z);

        int offsetX = (this.width - this.xSize) / 2;
        int offsetZ = (this.height - this.ySize) / 2;

        if (x >= 147 + offsetX && x <= 167 + offsetX && z >= 9 + offsetZ && z <= 70 + offsetZ) {
            long currentEnergy = EnergyDisplayUtil.type.formatEnergyForDisplay(energyHatch.getCurrentEnergy());
            long maxEnergy = EnergyDisplayUtil.type.formatEnergyForDisplay(energyHatch.getMaxEnergy());

            List<String> text = Lists.newArrayList();
            text.add(I18n.format("tooltip.energyhatch.charge",
                    MiscUtils.formatNumber(currentEnergy),
                    MiscUtils.formatNumber(maxEnergy),
                    I18n.format(EnergyDisplayUtil.type.getUnlocalizedFormat())));

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            drawHoveringText(text, x, z, (font == null ? fontRenderer : font));
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        drawInformation();

        float percFilled = ((float) energyHatch.getCurrentEnergy()) / ((float) energyHatch.getMaxEnergy());
        int pxFilled = MathHelper.ceil(percFilled * 61F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_ENERGY_HATCH);
        this.drawTexturedModalRect(147, 9 + 61 - pxFilled, 176, 61 - pxFilled, 20, pxFilled);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_ENERGY_HATCH);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
    }

    protected void drawInformation() {
        int offsetY = 12;
        var size = energyHatch.getTier();
        List<String> text = Lists.newArrayList();
        if (EnergyDisplayUtil.displayFETooltip) {
            text.add(I18n.format("tooltip.energyhatch.storage", MiscUtils.formatNumber(size.maxEnergy)));
            text.add(I18n.format("tooltip.energyhatch.limit", MiscUtils.formatNumber(size.transferLimit)));
        }
        if (Mods.IC2.isPresent() && EnergyDisplayUtil.displayIC2EUTooltip) {
            text.add("");
            text.add(I18n.format("tooltip.energyhatch.ic2.voltage",
                    I18n.format(size.getUnlocalizedEnergyDescriptor())));
            text.add(I18n.format("tooltip.energyhatch.ic2.transfer",
                    MiscUtils.formatDecimal(size.getIC2EnergyTransmission()),
                    I18n.format("tooltip.energyhatch.ic2.powerrate")));
        }
        if (Mods.GREGTECH.isPresent() && EnergyDisplayUtil.displayGTEUTooltip) {
            text.add("");
            addGTInfo(text, size);
        }

        float scale = 0.8f;
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);

        for (String s : text) {
            this.fontRenderer.drawStringWithShadow(s, 42, offsetY, 0xFFFFFF);
            offsetY += 8;
        }

        GlStateManager.popMatrix();
    }

    @Optional.Method(modid = "gregtech")
    protected void addGTInfo(List<String> text, EnergyHatchData size) {
        text.add(I18n.format("tooltip.energyhatch.gregtech.voltage",
                MiscUtils.formatDecimal(size.getGTEnergyTransferVoltage()),
                size.getUnlocalizedGTEnergyTier()));
        text.add(I18n.format("tooltip.energyhatch.gregtech.amperage",
                String.valueOf(size.getGtAmperage())));
        text.add(I18n.format("tooltip.energyhatch.gregtech.storage",
                MiscUtils.formatDecimal(EnergyDisplayUtil.EnergyType.GT_EU.formatEnergyForDisplay(size.maxEnergy))));
    }

}
