/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.container;

import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatchBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.voidstudio.mmce.common.item.ItemEnergyPowerModule;

import javax.annotation.Nonnull;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ContainerEnergyHatch
 * Created by HellFirePvP
 * Date: 09.07.2017 / 14:26
 */
public class ContainerEnergyHatch extends ContainerBase<TileEnergyHatchBase> {

    private final Slot slotPowerModule;

    public ContainerEnergyHatch(TileEnergyHatchBase owner, EntityPlayer opening) {
        super(owner, opening);

        this.slotPowerModule = addSlotToContainer(new SlotPowerModule(
                owner.getInventory().asGUIAccess(),
                TileEnergyHatch.POWER_MODULE_SLOT, 9, 9
        ));
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 36) {
                if (!itemstack1.isEmpty() && itemstack1.getItem() instanceof ItemEnergyPowerModule) {
                    Slot sb = this.inventorySlots.get(this.slotPowerModule.slotNumber);
                    if (!sb.getHasStack()) {
                        if (!this.mergeItemStack(itemstack1, sb.slotNumber, sb.slotNumber + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (index < 27) {
                if (!this.mergeItemStack(itemstack1, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 36) {
                if (!this.mergeItemStack(itemstack1, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, 36, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    public static class SlotPowerModule extends SlotItemHandler {

        public SlotPowerModule(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            if (!(stack.getItem() instanceof ItemEnergyPowerModule)) {
                return false;
            }
            return super.isItemValid(stack);
        }
    }
}
