package net.voidstudio.mmce.client.gui.button;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class SettingsButtonEnergyHatch extends GuiButton {
    protected static final ResourceLocation TEXTURE = new ResourceLocation(ModularMachinery.MODID, "textures/gui/energyhatch.png");

    public SettingsButtonEnergyHatch(int buttonId, int x, int y) {
        super(buttonId, x, y, 16, 16, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        this.hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width&& mouseY < this.y + this.height;

        mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int textureX = hovered ? 16 : 0;
        int textureY = 166;
        this.drawTexturedModalRect(x, y, textureX, textureY, width, height);
        this.drawTexturedModalRect(x, y, 0, 182, width, height);
        this.mouseDragged(mc, mouseX, mouseY);
    }
}
