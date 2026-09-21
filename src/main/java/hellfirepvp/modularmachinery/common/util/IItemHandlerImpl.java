package hellfirepvp.modularmachinery.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class IItemHandlerImpl implements IItemHandlerModifiable {
    public static final int DEFAULT_SLOT_LIMIT = 64;

    private static final int[] NO_SLOTS = new int[0];
    private static final EnumFacing[] NO_SIDES = new EnumFacing[0];

    protected int[] slotLimits; // Value not present means default, aka 64.
    protected SlotStackHolder[] inventory;

    // Only recipe snapshots use implicit empty slots. Live IOInventory storage stays dense.
    private boolean recipeSnapshot;
    private boolean implicitEmptySlots;
    private volatile SlotStackHolder[] publishedInventory;

    public boolean allowAnySlots = false;
    public EnumFacing[] accessibleSides = NO_SIDES;
    protected int[] inSlots = NO_SLOTS, outSlots = NO_SLOTS, miscSlots = NO_SLOTS;

    protected IItemHandlerImpl() {
        slotLimits = new int[]{DEFAULT_SLOT_LIMIT};
        inventory = new SlotStackHolder[]{new SlotStackHolder(0)};
    }

    private IItemHandlerImpl(IItemHandlerImpl source, boolean deepCopy) {
        this(source, deepCopy, false);
    }

    private IItemHandlerImpl(IItemHandlerImpl source, boolean deepCopy, boolean sparse) {
        recipeSnapshot = sparse;
        implicitEmptySlots = sparse;
        inSlots = source.inSlots;
        outSlots = source.outSlots;
        // Keep the existing distinction between deep and shallow copies, including slot access.
        int slots;
        if (deepCopy) {
            accessibleSides = Objects.requireNonNull(source.accessibleSides);
            slots = Math.max(getArrayMax(inSlots), getArrayMax(outSlots)) + 1;
            slotLimits = new int[slots];
            Arrays.fill(slotLimits, DEFAULT_SLOT_LIMIT);
        } else {
            miscSlots = source.miscSlots;
            slots = source.inventoryForRead().length;
            slotLimits = source.slotLimits;
        }

        inventory = new SlotStackHolder[slots];
        boolean hasImplicitEmptySlot = false;
        for (int i = 0; i < source.inventoryForRead().length; i++) {
            SlotStackHolder holder = source.holderAt(i);
            boolean sourceSlotIsImplicitEmpty = false;
            if (holder == null && source.recipeSnapshot) {
                // Classify a lazy empty slot atomically with structural materialization.
                // Do not hold the source lock across ItemStack/capability callbacks.
                synchronized (source) {
                    holder = source.holderAt(i);
                    sourceSlotIsImplicitEmpty = holder == null && source.implicitEmptySlots;
                }
            }
            if (sourceSlotIsImplicitEmpty) {
                if (!sparse) {
                    inventory[i] = new SlotStackHolder(i);
                } else {
                    hasImplicitEmptySlot = true;
                }
            } else if (sparse && holder.getClass() == SlotStackHolder.class && holder.slotId == i) {
                // Read each stack once, at the same
                // point as SlotStackHolder.copy(), and eagerly copy all occupied stacks.
                ItemStack stack = holder.itemStack.get();
                if (!stack.isEmpty()) {
                    SlotStackHolder copied = new SlotStackHolder(holder.slotId);
                    copied.itemStack.set(stack.copy());
                    inventory[i] = copied;
                } else {
                    hasImplicitEmptySlot = true;
                }
            } else {
                if (sparse) {
                    // Preserve custom holders' virtual copy contract, including null,
                    // and nonstandard slot IDs. Previously skipped slots become dense.
                    for (int previous = 0; previous < i; previous++) {
                        if (inventory[previous] == null) {
                            inventory[previous] = new SlotStackHolder(previous);
                        }
                    }
                    sparse = false;
                    recipeSnapshot = false;
                    implicitEmptySlots = false;
                }
                inventory[i] = deepCopy ? holder.copy() : holder.fastCopy();
            }
        }
        for (int i = source.inventoryForRead().length; i < slots; i++) {
            if (!sparse) {
                inventory[i] = new SlotStackHolder(i);
            } else {
                hasImplicitEmptySlot = true;
            }
        }
        if (deepCopy) {
            System.arraycopy(source.slotLimits, 0, slotLimits, 0, source.slotLimits.length);
        }
        if (sparse && hasImplicitEmptySlot) {
            publishedInventory = inventory;
        } else {
            // A full inventory gains nothing from sparse reads or synchronized writes.
            recipeSnapshot = false;
            implicitEmptySlots = false;
        }
    }

    /** Internal recipe path; public copy() and custom copy overrides retain dense semantics. */
    final IItemHandlerImpl copyForRecipe() {
        if (getClass() != IItemHandlerImpl.class && getClass() != IOInventory.class) {
            return copy();
        }
        SlotStackHolder[] current = inventoryForRead();
        // A full inventory cannot save holders. This allocation-free hint only selects
        // the implementation; the constructor still reads each actual snapshot stack.
        // Reference checks avoid additional ItemStack callbacks or isEmpty() calls.
        for (SlotStackHolder holder : current) {
            if (holder == null || holder.itemStack.get() == ItemStack.EMPTY) {
                int slots = Math.max(getArrayMax(inSlots), getArrayMax(outSlots)) + 1;
                if (current.length > slots || slotLimits.length > slots) {
                    return copy();
                }
                return new IItemHandlerImpl(this, true, true);
            }
        }
        return copy();
    }

    private SlotStackHolder[] inventoryForRead() {
        return recipeSnapshot ? publishedInventory : inventory;
    }

    private SlotStackHolder holderAt(int slot) {
        return inventoryForRead()[slot];
    }

    private SlotStackHolder writableHolderAt(int slot) {
        SlotStackHolder holder = holderAt(slot);
        if (holder != null || !recipeSnapshot) {
            return holder;
        }
        synchronized (this) {
            holder = holderAt(slot);
            if (holder == null && implicitEmptySlots) {
                holder = new SlotStackHolder(slot);
                inventory[slot] = holder;
                // Publish the newly installed holder. Subsequent stack writes still use
                // AtomicReference; simultaneous first writers cannot install two holders.
                publishedInventory = inventory;
            }
            return holder;
        }
    }

    private void materializeEmptySlots() {
        if (!recipeSnapshot) {
            return;
        }
        synchronized (this) {
            if (!implicitEmptySlots) {
                return;
            }
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i] == null) {
                    inventory[i] = new SlotStackHolder(i);
                }
            }
            implicitEmptySlots = false;
            publishedInventory = inventory;
        }
    }

    private void publishInventory() {
        if (recipeSnapshot) {
            publishedInventory = inventory;
        }
    }

    public IItemHandlerImpl(int[] inSlots, int[] outSlots) {
        this(inSlots, outSlots, EnumFacing.VALUES);
    }

    public IItemHandlerImpl(int[] inSlots, int[] outSlots, EnumFacing[] accessibleFrom) {
        this.inSlots = inSlots;
        this.outSlots = outSlots;

        int max = Math.max(getArrayMax(inSlots), getArrayMax(outSlots)) ;
        this.inventory = new SlotStackHolder[max + 1];
        this.slotLimits = new int[max + 1];

        Arrays.fill(this.slotLimits, DEFAULT_SLOT_LIMIT);
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = new SlotStackHolder(i);
        }

        this.accessibleSides = Objects.requireNonNull(accessibleFrom);
    }

    public IItemHandlerImpl(IItemHandlerModifiable handler) {
        this.slotLimits = new int[]{DEFAULT_SLOT_LIMIT};
        int slots = handler.getSlots();
        int[] inSlots = new int[slots];
        for (int i = 0; i < slots; i++) {
            inSlots[i] = i;
        }

        int[] outSlots = new int[slots];
        for (int i = 0; i < slots; i++) {
            outSlots[i] = i;
        }

        this.inSlots = inSlots;
        this.outSlots = outSlots;

        this.accessibleSides = EnumFacing.VALUES;
        this.inventory = new SlotStackHolder[slots];
        for (int i = 0; i < slots; i++) {
            SlotStackHolder holder = new SlotStackHolder(i);
            ItemStack stackInSlot = handler.getStackInSlot(i);
            if (stackInSlot.isEmpty()) {
                holder.itemStack.set(ItemStack.EMPTY);
            } else {
                holder.itemStack.set(stackInSlot.copy());
            }
            this.inventory[i] = holder;
        }
    }

    public IItemHandlerImpl copy() {
        return new IItemHandlerImpl(this, true);
    }

    public IItemHandlerImpl fastCopy() {
        return new IItemHandlerImpl(this, false);
    }

    protected static boolean arrayContains(int[] array, int i) {
        return Arrays.binarySearch(array, i) >= 0;
    }

    protected static boolean canMergeItemStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty() || other.isEmpty() || !stack.isStackable() || !other.isStackable()) {
            return false;
        }
        return stack.isItemEqual(other) && ItemStack.areItemStackTagsEqual(stack, other);
    }

    public IItemHandlerImpl setMiscSlots(int... miscSlots) {
        materializeEmptySlots();
        this.miscSlots = miscSlots;

        int max = getArrayMax(miscSlots);
        checkSlotLimitsLength(max);
        checkInventoryLength(max);

        for (int slot : miscSlots) {
            this.inventory[slot] = new SlotStackHolder(slot);
        }
        publishInventory();
        return this;
    }

    public IItemHandlerImpl setStackLimit(int limit, int... slots) {
        materializeEmptySlots();
        int max = getArrayMax(slots);
        checkSlotLimitsLength(max);
        checkInventoryLength(max);

        for (int slot : slots) {
            this.slotLimits[slot] = limit;
        }
        return this;
    }

    public IItemHandlerModifiable asGUIAccess() {
        return new GuiAccess(this);
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        if (slot <= -1 || slot >= inventoryForRead().length) {
            return;
        }
        writableHolderAt(slot).itemStack.set(stack);
    }

    @Override
    public int getSlots() {
        return inventoryForRead().length;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < 0 || slot >= slotLimits.length) {
            return DEFAULT_SLOT_LIMIT;
        }
        return slotLimits[slot];
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= inventoryForRead().length) {
            return ItemStack.EMPTY;
        }
        SlotStackHolder holder = holderAt(slot);
        if (holder != null) {
            return holder.itemStack.get();
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        return insertItemInternal(slot, stack, simulate);
    }

    protected ItemStack insertItemInternal(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (!allowAnySlots) {
            if (!arrayContains(inSlots, slot)) {
                return stack;
            }
        }

        IItemHandlerImpl.SlotStackHolder holder = writableHolderAt(slot);
        if (holder == null) {
            return stack; // Shouldn't happen anymore here tho
        }
        ItemStack toInsert = ItemUtils.copyStackWithSize(stack, stack.getCount());
        if (!holder.itemStack.get().isEmpty()) {
            ItemStack existing = ItemUtils.copyStackWithSize(holder.itemStack.get(), holder.itemStack.get().getCount());
            int max = Math.min(existing.getMaxStackSize(), getSlotLimit(slot));
            if (existing.getCount() >= max || !canMergeItemStacks(existing, toInsert)) {
                return stack;
            }
            int movable = Math.min(max - existing.getCount(), stack.getCount());
            if (!simulate) {
                holder.itemStack.get().grow(movable);
            }
            if (movable >= stack.getCount()) {
                return ItemStack.EMPTY;
            } else {
                ItemStack copy = stack.copy();
                copy.shrink(movable);
                return copy;
            }
        } else {
            int max = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
            if (max >= stack.getCount()) {
                if (!simulate) {
                    holder.itemStack.set(stack.copy());
                }
                return ItemStack.EMPTY;
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(max);
                if (!simulate) {
                    holder.itemStack.set(copy);
                }
                copy = stack.copy();
                copy.shrink(max);
                return copy;
            }
        }
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractItemInternal(slot, amount, simulate);
    }

    protected ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        if (!allowAnySlots) {
            if (!arrayContains(outSlots, slot)) {
                return ItemStack.EMPTY;
            }
        }
        IItemHandlerImpl.SlotStackHolder holder = holderAt(slot);
        if (holder == null) {
            return ItemStack.EMPTY; // Shouldn't happen anymore here tho
        }
        if (holder.itemStack.get().isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extract = ItemUtils.copyStackWithSize(holder.itemStack.get(), Math.min(amount, holder.itemStack.get().getCount()));
        if (extract.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            holder.itemStack.set(ItemUtils.copyStackWithSize(holder.itemStack.get(), holder.itemStack.get().getCount() - extract.getCount()));
        }
        return extract;
    }

    public void clear() {
        if (recipeSnapshot) {
            synchronized (this) {
                clearHolders();
            }
        } else {
            clearHolders();
        }
    }

    private void clearHolders() {
        for (final SlotStackHolder holder : inventoryForRead()) {
            if (holder != null || !implicitEmptySlots) {
                holder.itemStack.set(ItemStack.EMPTY);
            }
        }
    }

    public boolean hasCapability(EnumFacing facing) {
        return facing == null || Arrays.binarySearch(accessibleSides, facing) >= 0;
    }

    public IItemHandlerModifiable getCapability(EnumFacing facing) {
        if (hasCapability(facing)) {
            return this;
        }
        return null;
    }

    protected void checkSlotLimitsLength(final int max) {
        int required = max + 1;
        int invLength = slotLimits.length;
        if (required > invLength) {
            int[] tmp = new int[required];
            Arrays.fill(tmp, invLength, max, DEFAULT_SLOT_LIMIT);
            System.arraycopy(slotLimits, 0, tmp, 0, invLength);
            this.slotLimits = tmp;
        }
    }

    protected void checkInventoryLength(final int max) {
        int required = max + 1;
        int invLength = inventory.length;
        if (required > invLength) {
            SlotStackHolder[] tmp = new SlotStackHolder[required];
            for (int i = invLength; i < max; i++) {
                tmp[i] = new SlotStackHolder(i);
            }
            System.arraycopy(inventory, 0, tmp, 0, invLength);
            this.inventory = tmp;
            publishInventory();
        }
    }

    protected static int getArrayMax(final int[] slots) {
        int max = 0;
        for (final int slot : slots) {
            if (slot > max) {
                max = slot;
            }
        }
        return max;
    }

    public static class SlotStackHolder {

        public final int slotId;

        public final AtomicReference<ItemStack> itemStack = new AtomicReference<>(ItemStack.EMPTY);

        public SlotStackHolder(int slotId) {
            this.slotId = slotId;
        }

        public SlotStackHolder copy() {
            SlotStackHolder copied = new SlotStackHolder(slotId);
            ItemStack holderStack = itemStack.get();
            if (!holderStack.isEmpty()) {
                copied.itemStack.set(holderStack.copy());
            }
            return copied;
        }

        public SlotStackHolder fastCopy() {
            SlotStackHolder copied = new SlotStackHolder(slotId);
            ItemStack holderStack = itemStack.get();
            if (!holderStack.isEmpty()) {
                copied.itemStack.set(holderStack);
            }
            return copied;
        }
    }

    public static class GuiAccess implements IItemHandlerModifiable {

        private final IItemHandlerImpl inventory;

        public GuiAccess(IItemHandlerImpl inventory) {
            this.inventory = inventory;
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            inventory.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            boolean allowPrev = inventory.allowAnySlots;
            inventory.allowAnySlots = true;

            ItemStack insert = inventory.insertItem(slot, stack, simulate);

            inventory.allowAnySlots = allowPrev;
            return insert;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            boolean allowPrev = inventory.allowAnySlots;
            inventory.allowAnySlots = true;

            ItemStack extract = inventory.extractItem(slot, Math.min(amount, 64), simulate);

            inventory.allowAnySlots = allowPrev;
            return extract;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }
    }
}
