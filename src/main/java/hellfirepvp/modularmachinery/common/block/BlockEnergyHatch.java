package hellfirepvp.modularmachinery.common.block;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatchBase;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.RedstoneHelper;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.voidstudio.mmce.common.item.ItemEnergyPowerModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatchBase.POWER_MODULE_SLOT;

public abstract class BlockEnergyHatch extends BlockMachineComponent {

    public BlockEnergyHatch() {
        super(Material.IRON);
        setHardness(2F);
        setResistance(10F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        tooltip.add(I18n.format("tooltip.energyhatch.info"));
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return RedstoneHelper.getRedstoneLevel(worldIn.getTileEntity(pos));
    }

    @Nullable
    @Override
    public abstract TileEntity createTileEntity(World world, IBlockState state);

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return null;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        ItemStack heldStack = playerIn.getHeldItem(hand);
        TileEntity te = worldIn.getTileEntity(pos);

        if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemEnergyPowerModule
                && te instanceof TileEnergyHatchBase hatch) {
            IOInventory inv = hatch.getInventory();
            ItemStack stackInSlot = inv.getStackInSlot(POWER_MODULE_SLOT);
            ItemStack stackToInsert = heldStack.splitStack(1);

            if (!stackInSlot.isEmpty() && !playerIn.addItemStackToInventory(stackInSlot)) {
                spawnAsEntity(playerIn.world, playerIn.getPosition(), stackInSlot);
            }
            inv.setStackInSlot(POWER_MODULE_SLOT, stackToInsert);
            return true;
        }

        onBlockActivated(playerIn, te, worldIn, pos);
        return true;
    }

    @Override
    public void breakBlock(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        if (worldIn.getTileEntity(pos) instanceof TileEnergyHatchBase hatch) {
            var inv = hatch.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                var stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    spawnAsEntity(worldIn, pos, stack);
                    inv.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    /**
     * Use to open gui. Calls in server side
     *
     * @param player player
     * @param tile   tile when opened
     * @param world  world
     * @param pos    pos
     */
    public void onBlockActivated(EntityPlayer player, TileEntity tile, World world, BlockPos pos) {
        if (tile instanceof TileEnergyHatch) {
            player.openGui(ModularMachinery.MODID, CommonProxy.GuiType.ENERGY_INVENTORY.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
