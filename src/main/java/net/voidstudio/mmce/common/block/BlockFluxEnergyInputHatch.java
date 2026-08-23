package net.voidstudio.mmce.common.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.voidstudio.mmce.common.tiles.TileFluxEnergyInputHatch;

import javax.annotation.Nullable;
import java.util.List;

public class BlockFluxEnergyInputHatch extends BlockFluxEnergyHatch {

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        tooltip.add(I18n.format("tooltip.fluxenergyinputhatch.info"));
        super.addInformation(stack, player, tooltip, advanced);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileFluxEnergyInputHatch();
    }

}
