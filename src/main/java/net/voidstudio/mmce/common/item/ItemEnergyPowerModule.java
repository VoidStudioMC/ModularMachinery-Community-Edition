package net.voidstudio.mmce.common.item;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemEnergyPowerModule extends Item implements ItemVariant {
    public static final ItemEnergyPowerModule INSTANCE = new ItemEnergyPowerModule();

    private ItemEnergyPowerModule() {
        setRegistryName(new ResourceLocation(ModularMachinery.MODID, "energy_power_module"));
        setTranslationKey(ModularMachinery.MODID + ".energy_power_module");
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setHasSubtypes(true);
    }

    @Nonnull
    @Override
    public String getTranslationKey(@Nonnull ItemStack stack) {
        var size = EnergyHatchData.values()[stack.getMetadata()];
        return super.getTranslationKey(stack) + "." + size.getName();
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for (int i = 1; i < EnergyHatchData.values().length; i++) {
                items.add(new ItemStack(this, 1, i));
            }
        }
    }

    @Override
    public String getVariantName(int metadata) {
        return EnergyHatchData.values()[metadata].getName();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        var size = EnergyHatchData.values()[stack.getMetadata()];
        if (EnergyDisplayUtil.displayFETooltip) {
            tooltip.add(I18n.format("tooltip.energyhatch.storage", MiscUtils.formatDecimal(size.maxEnergy)));
            tooltip.add(I18n.format("tooltip.energyhatch.limit", MiscUtils.formatDecimal(size.transferLimit)));
        }
        if (Mods.IC2.isPresent() && EnergyDisplayUtil.displayIC2EUTooltip) {
            tooltip.add("");
            tooltip.add(I18n.format("tooltip.energyhatch.ic2.voltage",
                    I18n.format(size.getUnlocalizedEnergyDescriptor())));
            tooltip.add(I18n.format("tooltip.energyhatch.ic2.transfer",
                    MiscUtils.formatDecimal(size.getIC2EnergyTransmission()),
                    I18n.format("tooltip.energyhatch.ic2.powerrate")));
        }
        if (Mods.GREGTECH.isPresent() && EnergyDisplayUtil.displayGTEUTooltip) {
            tooltip.add("");
            addGTTooltip(tooltip, size);
        }
    }

    @Optional.Method(modid = "gregtech")
    protected void addGTTooltip(List<String> tooltip, EnergyHatchData size) {
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.energyhatch.gregtech.voltage",
                MiscUtils.formatDecimal(size.getGTEnergyTransferVoltage()),
                size.getUnlocalizedGTEnergyTier()));
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.energyhatch.gregtech.amperage",
                String.valueOf(size.getGtAmperage())));
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.energyhatch.gregtech.storage",
                MiscUtils.formatDecimal(EnergyDisplayUtil.EnergyType.GT_EU.formatEnergyForDisplay(size.maxEnergy))));
    }
}
