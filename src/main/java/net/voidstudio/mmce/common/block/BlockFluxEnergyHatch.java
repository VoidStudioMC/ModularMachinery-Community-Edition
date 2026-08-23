package net.voidstudio.mmce.common.block;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockEnergyHatch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.voidstudio.mmce.common.tiles.base.TileFluxEnergyHatch;
import sonar.fluxnetworks.api.translate.FluxTranslate;
import sonar.fluxnetworks.common.item.ItemConfigurator;

import javax.annotation.ParametersAreNonnullByDefault;

public abstract class BlockFluxEnergyHatch extends BlockEnergyHatch {

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        if (playerIn.getHeldItem(hand).getItem() instanceof ItemConfigurator) {
            return false;
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public void onBlockActivated(EntityPlayer player, TileEntity tile, World world, BlockPos pos) {
        if (!(tile instanceof TileFluxEnergyHatch hatch)) {
            return;
        }

        if (hatch.isPlayerUsing()) {
            TextComponentTranslation textComponents = new TextComponentTranslation(FluxTranslate.ACCESS_OCCUPY_KEY);
            textComponents.getStyle().setBold(true);
            textComponents.getStyle().setColor(TextFormatting.DARK_RED);
            player.sendStatusMessage(textComponents, true);
            return;
        }

        if (hatch.canAccess(player)) {
            player.openGui(ModularMachinery.MODID, CommonProxy.GuiType.FLUX_ENERGY_HATCH.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
            return;
        }

        TextComponentTranslation textComponents = new TextComponentTranslation(FluxTranslate.ACCESS_DENIED_KEY);
        textComponents.getStyle().setBold(true);
        textComponents.getStyle().setColor(TextFormatting.DARK_RED);
        player.sendStatusMessage(textComponents, true);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        if (worldIn.isRemote) {
            return;
        }

        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileFluxEnergyHatch hatch) {
            if (placer instanceof EntityPlayer) {
                hatch.setConnectionOwner(EntityPlayer.getUUID(((EntityPlayer) placer).getGameProfile()));
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (world.isRemote) {
            return false;
        }

        TileEntity tile = world.getTileEntity(pos);

        if (tile instanceof TileFluxEnergyHatch hatch) {
            if (hatch.canAccess(player)) {
                ItemStack stack = new ItemStack(this, 1, damageDropped(state));

                float motion = 0.7F;
                double motionX = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
                double motionY = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
                double motionZ = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;

                EntityItem entityItem = new EntityItem(world, pos.getX() + motionX, pos.getY() + motionY, pos.getZ() + motionZ, stack);

                onBlockHarvested(world, pos, state, player);
                world.setBlockToAir(pos);
                world.spawnEntity(entityItem);
                return true;
            }
        }

        TextComponentTranslation textComponents = new TextComponentTranslation(FluxTranslate.REMOVAL_DENIED_KEY);
        textComponents.getStyle().setBold(true);
        textComponents.getStyle().setColor(TextFormatting.DARK_RED);
        player.sendStatusMessage(textComponents, true);
        return false;
    }
}
