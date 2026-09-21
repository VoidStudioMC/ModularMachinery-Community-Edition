/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.common.helper.AdvancedItemChecker;
import github.kasuminova.mmce.common.util.OredictCache;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.nbt.NBTMatchingHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemUtils
 * Created by HellFirePvP
 * Date: 28.06.2017 / 12:42
 */
public class ItemUtils {

    public static void decrStackInInventory(ItemStackHandler handler, int slot) {
        if (slot < 0 || slot >= handler.getSlots()) return;
        ItemStack st = handler.getStackInSlot(slot);
        if (st.isEmpty()) return;
        st.setCount(st.getCount() - 1);
        if (st.getCount() <= 0) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    //Negative amount: overhead fuel burnt
    //Positive amount: Failure/couldn't find enough fuel
    public static int consumeFromInventoryFuel(IItemHandlerModifiable handler, int fuelAmtToConsume, boolean simulate, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventoryFuel(handler, matchNBTTag, true);
        if (contents.isEmpty()) {
            return fuelAmtToConsume;
        }

        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (inSlot.getCount() > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ForgeHooks.getContainerItem(inSlot);
                fuelAmtToConsume -= TileEntityFurnace.getItemBurnTime(inSlot);
                if (!simulate) {
                    handler.setStackInSlot(slot, stack.copy());
                }
                if (fuelAmtToConsume <= 0) {
                    break;
                }
            }
            int fuelPer = TileEntityFurnace.getItemBurnTime(inSlot);
            int toConsumeDiv = fuelAmtToConsume / fuelPer;
            int fuelMod = fuelAmtToConsume % fuelPer;

            int toConsume = toConsumeDiv + (fuelMod > 0 ? 1 : 0);
            int toRemove = Math.min(toConsume, inSlot.getCount());

            fuelAmtToConsume -= toRemove * fuelPer;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (fuelAmtToConsume <= 0) {
                break;
            }
        }
        return fuelAmtToConsume;
    }

    public static boolean consumeFromInventory(IItemHandlerModifiable handler, ItemStack toConsume, boolean simulate, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, matchNBTTag, true);
        if (contents.isEmpty()) return false;

        int cAmt = toConsume.getCount();
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (inSlot.getCount() > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ForgeHooks.getContainerItem(inSlot);
                cAmt--;
                if (!simulate) {
                    handler.setStackInSlot(slot, stack.copy());
                }
                if (cAmt <= 0) {
                    break;
                }
            }
            int toRemove = Math.min(cAmt, inSlot.getCount());
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    public static boolean consumeFromInventory(IItemHandlerModifiable handler, ItemStack toConsume, boolean simulate, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, itemChecker, controller, true);
        if (contents.isEmpty()) return false;

        int cAmt = toConsume.getCount();
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (inSlot.getCount() > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ForgeHooks.getContainerItem(inSlot);
                cAmt--;
                if (!simulate) {
                    handler.setStackInSlot(slot, stack.copy());
                }
                if (cAmt <= 0) {
                    break;
                }
            }
            int toRemove = Math.min(cAmt, inSlot.getCount());
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    public static int consumeAll(IItemHandlerModifiable handler, ItemStack toConsume, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, itemChecker, controller, true);
        if (toConsume.getCount() <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, toConsume.getCount());
    }

    public static int consumeAll(IItemHandlerModifiable handler, ItemStack toConsume, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, matchNBTTag, true);
        if (toConsume.getCount() <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, toConsume.getCount());
    }

    public static int consumeAll(IItemHandlerModifiable handler, String oreName, int amount, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, itemChecker, controller, true);
        if (amount <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, amount);
    }

    public static int consumeAll(IItemHandlerModifiable handler, String oreName, int amount, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, matchNBTTag, true);
        if (amount <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, amount);
    }

    public static int insertAll(@Nonnull ItemStack stack, IItemHandlerModifiable handler, int maxInsert) {
        if (stack.getCount() <= 0) {
            return 0;
        }

        int inserted = 0;
        boolean isStackable = stack.isStackable();

        for (int i = 0; i < handler.getSlots(); i++) {
            int maxStackSize = handler.getSlotLimit(i);
            ItemStack in = handler.getStackInSlot(i);
            int count = in.getCount();

            // skip full slots
            if (count >= maxStackSize) {
                continue;
            }

            if (in.isEmpty()) {
                // only one for non-stackable
                int toInsert = isStackable ? Math.min(maxInsert - inserted, maxStackSize) : 1;

                handler.setStackInSlot(i, copyStackWithSize(stack, toInsert));
                inserted += toInsert;
            }
            // non-empty with match
            else if (stackEqualsNonNBT(stack, in) && matchTags(stack, in)) {
                // for non-stackable, skip if the slot is occupied
                if (!isStackable) {
                    continue;
                }

                int toInsert = Math.min(maxInsert - inserted, maxStackSize - count);
                handler.setStackInSlot(i, copyStackWithSize(stack, toInsert + count));
                inserted += toInsert;
            }

            // if all inserted - break
            if (inserted >= maxInsert) {
                break;
            }
        }

        return inserted;
    }

    private static int consumeAllInternal(IItemHandlerModifiable handler, Int2ObjectMap<ItemStack> contents, int maxConsume) {
        int cAmt = 0;
        var entries = contents.int2ObjectEntrySet();
        var iterator = entries instanceof Int2ObjectMap.FastEntrySet
                ? ((Int2ObjectMap.FastEntrySet<ItemStack>) entries).fastIterator() : entries.iterator();
        while (iterator.hasNext()) {
            final Int2ObjectMap.Entry<ItemStack> content = iterator.next();
            int slot = content.getIntKey();
            ItemStack stack = content.getValue();
            int count = stack.getCount();
            if (count > 1) {
                if (stack.getItem().hasContainerItem(stack)) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
            }

            int toConsume = Math.min(maxConsume - cAmt, count);
            handler.setStackInSlot(slot, copyStackWithSize(stack, count - toConsume));
            cAmt += toConsume;

            if (cAmt >= maxConsume) {
                break;
            }
        }

        return cAmt;
    }

    public static boolean stackEqualsNonNBT(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty() && other.isEmpty())
            return true;
        if (stack.isEmpty() || other.isEmpty())
            return false;
        Item sItem = stack.getItem();
        Item oItem = other.getItem();
        if (sItem.getHasSubtypes() || oItem.getHasSubtypes()) {
            return sItem.equals(other.getItem()) &&
                   (stack.getItemDamage() == other.getItemDamage() ||
                    stack.getItemDamage() == OreDictionary.WILDCARD_VALUE ||
                    other.getItemDamage() == OreDictionary.WILDCARD_VALUE);
        } else {
            return sItem.equals(other.getItem());
        }
    }

    public static boolean matchTags(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        return ItemStack.areItemStackTagsEqual(stack, other);
    }

    @Nonnull
    public static ItemStack copyStackWithSize(@Nonnull ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack s = stack.copy();
        s.setCount(amount);
        return s;
    }

    /**
     * 向指定容器插入指定的物品，返回未插入的物品。
     *
     * @param external 容器
     * @param willBeInserted 要插入的物品
     * @return 未被插入的物品，如果全部插入，返回空物品
     */
    public static ItemStack insertItemStackToContainer(IItemHandler external, ItemStack willBeInserted) {
        ItemStack beInserted = willBeInserted;
        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack stackInSlot = external.getStackInSlot(externalSlotId);

            if (stackInSlot.isEmpty()) {
                ItemStack notInserted = external.insertItem(externalSlotId, beInserted, false);
                if (notInserted.isEmpty()) {
                    return ItemStack.EMPTY;
                } else {
                    beInserted = notInserted;
                    continue;
                }
            }

            if (matchStacks(stackInSlot, willBeInserted)) {
                ItemStack notInserted = external.insertItem(externalSlotId, beInserted, false);
                if (notInserted.isEmpty()) {
                    return ItemStack.EMPTY;
                } else {
                    beInserted = notInserted;
                }
            }
        }

        return beInserted;
    }

    // Buffer the first match. Two or more matches retain the original capacity and
    // insertion order. Only internal consumers use compact empty/singleton maps;
    // public callers retain the original capacity even if they later add more entries.
    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryFuel(IItemHandlerModifiable handler, @Nullable NBTTagCompound matchNBTTag) {
        return findItemsIndexedInInventoryFuel(handler, matchNBTTag, false);
    }

    private static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryFuel(IItemHandlerModifiable handler, @Nullable NBTTagCompound matchNBTTag, boolean compactSingleton) {
        int expectedSize = handler.getSlots() * 2;
        Int2ObjectMap<ItemStack> stacksOut = null;
        int firstSlot = -1;
        ItemStack firstStack = null;
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (TileEntityFurnace.getItemBurnTime(s) > 0 && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                if (firstSlot == -1) {
                    firstSlot = j;
                    firstStack = s;
                } else {
                    if (stacksOut == null) {
                        stacksOut = new Int2ObjectOpenHashMap<>(expectedSize);
                        stacksOut.put(firstSlot, firstStack);
                    }
                    stacksOut.put(j, s);
                }
            }
        }
        return stacksOut != null ? stacksOut : singleIndexedItem(expectedSize, compactSingleton, firstSlot, firstStack);
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryOreDict(IItemHandlerModifiable handler, String oreDict, @Nullable NBTTagCompound matchNBTTag) {
        return findItemsIndexedInInventoryOreDict(handler, oreDict, matchNBTTag, false);
    }

    private static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryOreDict(IItemHandlerModifiable handler, String oreDict, @Nullable NBTTagCompound matchNBTTag, boolean compactSingleton) {
        int expectedSize = handler.getSlots() * 2;
        Int2ObjectMap<ItemStack> stacksOut = null;
        int firstSlot = -1;
        ItemStack firstStack = null;
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (s.isEmpty()) continue;
            int[] ids = OredictCache.getOreIDsFast(s);
            for (int id : ids) {
                if (OreDictionary.getOreName(id).equals(oreDict) && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                    if (firstSlot == -1) {
                        firstSlot = j;
                        firstStack = s;
                    } else {
                        if (stacksOut == null) {
                            stacksOut = new Int2ObjectOpenHashMap<>(expectedSize);
                            stacksOut.put(firstSlot, firstStack);
                        }
                        stacksOut.put(j, s);
                    }
                    break;
                }
            }
        }
        return stacksOut != null ? stacksOut : singleIndexedItem(expectedSize, compactSingleton, firstSlot, firstStack);
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryOreDict(IItemHandlerModifiable handler, String oreDict, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        return findItemsIndexedInInventoryOreDict(handler, oreDict, itemChecker, controller, false);
    }

    private static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryOreDict(IItemHandlerModifiable handler, String oreDict, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller, boolean compactSingleton) {
        int expectedSize = handler.getSlots() * 2;
        Int2ObjectMap<ItemStack> stacksOut = null;
        int firstSlot = -1;
        ItemStack firstStack = null;
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (s.isEmpty()) continue;
            int[] ids = OredictCache.getOreIDsFast(s);
            for (int id : ids) {
                if (OreDictionary.getOreName(id).equals(oreDict) && itemChecker.isMatch(controller, s)) {
                    if (firstSlot == -1) {
                        firstSlot = j;
                        firstStack = s;
                    } else {
                        if (stacksOut == null) {
                            stacksOut = new Int2ObjectOpenHashMap<>(expectedSize);
                            stacksOut.put(firstSlot, firstStack);
                        }
                        stacksOut.put(j, s);
                    }
                    break;
                }
            }
        }
        return stacksOut != null ? stacksOut : singleIndexedItem(expectedSize, compactSingleton, firstSlot, firstStack);
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventory(IItemHandlerModifiable handler, ItemStack match, boolean strict, @Nullable NBTTagCompound matchNBTTag) {
        return findItemsIndexedInInventory(handler, match, strict, matchNBTTag, false);
    }

    private static Int2ObjectMap<ItemStack> findItemsIndexedInInventory(IItemHandlerModifiable handler, ItemStack match, boolean strict, @Nullable NBTTagCompound matchNBTTag, boolean compactSingleton) {
        int expectedSize = handler.getSlots() * 2;
        Int2ObjectMap<ItemStack> stacksOut = null;
        int firstSlot = -1;
        ItemStack firstStack = null;
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if ((strict ? matchStacks(s, match) : matchStackLoosely(s, match)) && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                if (firstSlot == -1) {
                    firstSlot = j;
                    firstStack = s;
                } else {
                    if (stacksOut == null) {
                        stacksOut = new Int2ObjectOpenHashMap<>(expectedSize);
                        stacksOut.put(firstSlot, firstStack);
                    }
                    stacksOut.put(j, s);
                }
            }
        }
        return stacksOut != null ? stacksOut : singleIndexedItem(expectedSize, compactSingleton, firstSlot, firstStack);
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventory(IItemHandlerModifiable handler, ItemStack match, boolean strict, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        return findItemsIndexedInInventory(handler, match, strict, itemChecker, controller, false);
    }

    private static Int2ObjectMap<ItemStack> findItemsIndexedInInventory(IItemHandlerModifiable handler, ItemStack match, boolean strict, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller, boolean compactSingleton) {
        int expectedSize = handler.getSlots() * 2;
        Int2ObjectMap<ItemStack> stacksOut = null;
        int firstSlot = -1;
        ItemStack firstStack = null;
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if ((strict ? matchStacks(s, match) : matchStackLoosely(s, match)) && itemChecker.isMatch(controller, s)) {
                if (firstSlot == -1) {
                    firstSlot = j;
                    firstStack = s;
                } else {
                    if (stacksOut == null) {
                        stacksOut = new Int2ObjectOpenHashMap<>(expectedSize);
                        stacksOut.put(firstSlot, firstStack);
                    }
                    stacksOut.put(j, s);
                }
            }
        }
        return stacksOut != null ? stacksOut : singleIndexedItem(expectedSize, compactSingleton, firstSlot, firstStack);
    }

    private static Int2ObjectMap<ItemStack> singleIndexedItem(int expectedSize, boolean compactSingleton,
                                                               int slot, ItemStack stack) {
        Int2ObjectMap<ItemStack> result = new Int2ObjectOpenHashMap<>(compactSingleton ? (slot < 0 ? 0 : 1) : expectedSize);
        if (slot >= 0) {
            result.put(slot, stack);
        }
        return result;
    }

    public static boolean matchStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (!ItemStack.areItemsEqual(stack, other)) return false;
        return ItemStack.areItemStackTagsEqual(stack, other);
    }

    public static boolean matchStackLoosely(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty()) return other.isEmpty();
        return OreDictionary.itemMatches(other, stack, false);
    }

    public static boolean stackNotInList(final List<ItemStack> list, final ItemStack stackFromBlockState) {
        for (final ItemStack stack : list) {
            if (matchStacks(stackFromBlockState, stack)) {
                return false;
            }
        }
        return true;
    }

    public static ItemStack getOredictItem(final RecipeCraftingContext context, final String oreDictName, final NBTTagCompound tag) {
        ItemStack stack = ItemStack.EMPTY;
        for (ItemStack oreInstance : OreDictionary.getOres(oreDictName)) {
            if (oreInstance.isEmpty()) {
                continue;
            }
            stack = copyStackWithSize(oreInstance, 1);

            if (!stack.isEmpty()) { //Try all options first..
                break;
            }
        }

        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Unknown ItemStack: Cannot find an item in oredict '" + oreDictName + "'!");
        }

        if (tag != null) {
            stack.setTagCompound(tag.copy());
        }
        return stack;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static List<ProcessingComponent<?>> copyItemHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>(components.size());
        for (ProcessingComponent<?> component : components) {
            Object provided = component.getProvidedComponent();
            IItemHandlerImpl handler = null;

            if (provided instanceof IItemHandlerImpl handlerMM) {
                handler = handlerMM.copyForRecipe();
            } else if (provided instanceof IItemHandlerModifiable handlerDefault) {
                handler = new IItemHandlerImpl(handlerDefault);
            }

            if (handler != null) {
                list.add(new ProcessingComponent<>(
                        (MachineComponent<Object>) component.component(),
                        handler,
                        component.getTag())
                );
            }
        }
        return list;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static List<ProcessingComponent<?>> fastCopyItemHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>(components.size());
        for (ProcessingComponent<?> component : components) {
            ProcessingComponent<Object> objectProcessingComponent = new ProcessingComponent<>(
                    (MachineComponent<Object>) component.component(),
                    ((IItemHandlerImpl) component.getProvidedComponent()).fastCopy(),
                    component.getTag());
            list.add(objectProcessingComponent);
        }
        return list;
    }
}
