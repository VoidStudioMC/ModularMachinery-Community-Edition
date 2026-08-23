package net.voidstudio.mmce.common.tiles.base;

import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTileNotifiable;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatchBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.api.network.ITransferHandler;
import sonar.fluxnetworks.api.tiles.IFluxConfigurable;
import sonar.fluxnetworks.api.tiles.IFluxConnector;
import sonar.fluxnetworks.api.utils.Coord4D;
import sonar.fluxnetworks.api.utils.NBTType;
import sonar.fluxnetworks.common.connection.FluxNetworkInvalid;
import sonar.fluxnetworks.common.core.FluxUtils;
import sonar.fluxnetworks.common.data.FluxChunkManager;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public abstract class TileFluxEnergyHatch extends TileEnergyHatchBase implements
        MachineComponentTileNotifiable,
        IFluxConnector,
        IFluxConfigurable,
        ITickable {

    protected HashSet<EntityPlayer> playerUsing = new HashSet<>();

    protected ItemStack displayStack = ItemStack.EMPTY;
    protected boolean isCustomName;
    protected String customName = "";

    protected UUID playerUUID = FluxUtils.UUID_DEFAULT;
    protected int folderID = -1;

    protected int priority = 0;
    protected long limit = FluxConfig.defaultLimit;

    protected boolean surgeMode = false;
    protected boolean disableLimit = true;

    protected boolean chunkLoading = false;

    protected IFluxNetwork network = FluxNetworkInvalid.instance;
    protected int networkID = -1;

    private Coord4D coord4D;

    private final ITransferHandler transferHandler = new TransferHandler();
    private boolean load;
    private final AtomicReference<String> connectedMachine = new AtomicReference<>("");
    private String machineName;

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        if (!playerUsing.isEmpty()) {
            notifyFluxUpdate();
        }
        if (!load) {
            if (!FluxUtils.addConnection(this)) {
                networkID = -1;
            }
            notifyFluxUpdate();
            load = true;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (!world.isRemote && load) {
            FluxUtils.removeConnection(this, false);
            if (chunkLoading) {
                FluxChunkManager.releaseChunk(world, new ChunkPos(pos));
            }
            getTransferHandler().reset();
            load = false;
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (!world.isRemote && load) {
            FluxUtils.removeConnection(this, true);
            getTransferHandler().reset();
            load = false;
        }
    }

    //================Machine NBT================

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        NBTTagCompound fluxNbt = new NBTTagCompound();
        writeFluxNBT(fluxNbt);
        compound.setTag("fluxNBT", fluxNbt);
        compound.setTag("displayStack", displayStack.serializeNBT());
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readFluxNBT(compound.getCompoundTag("fluxNBT"));
        this.displayStack = new ItemStack(compound.getCompoundTag("displayStack"));
    }

    //================Network NBT================

    @Override
    public NBTTagCompound writeCustomNBT(NBTTagCompound compound, NBTType type) {
        writeFluxNBT(compound);
        return compound;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound, NBTType type) {
        readFluxNBT(compound);
    }

    //================Flux NBT================

    private void writeFluxNBT(NBTTagCompound compound) {
        compound.setInteger("networkID", networkID);
        compound.setUniqueId("playerUUID", playerUUID);
        compound.setInteger("folderID", folderID);

        compound.setInteger("priority", priority);
        compound.setBoolean("surgeMode", surgeMode);

        compound.setLong("limit", limit);
        compound.setBoolean("disableLimit", disableLimit);

        compound.setBoolean("isCustomName", isCustomName);
        compound.setString("customName", customName);

        compound.setBoolean("chunkLoading", chunkLoading);
        getTransferHandler().writeCustomNBT(compound, NBTType.ALL_SAVE);
    }

    private void readFluxNBT(NBTTagCompound compound) {
        this.networkID = compound.getInteger("networkID");
        this.playerUUID = compound.getUniqueId("playerUUID");
        this.folderID = compound.getInteger("folderID");

        this.priority = compound.getInteger("priority");
        this.surgeMode = compound.getBoolean("surgeMode");

        this.limit = compound.getLong("limit");
        this.disableLimit = compound.getBoolean("disableLimit");

        this.isCustomName = compound.getBoolean("isCustomName");
        this.customName = compound.getString("customName");

        this.chunkLoading = compound.getBoolean("chunkLoading");
        getTransferHandler().readCustomNBT(compound, NBTType.ALL_SAVE);
    }

    //========================================

    @Override
    public int getLogicPriority() {
        return surgeMode ? Integer.MAX_VALUE : priority;
    }

    @Override
    public int getRawPriority() {
        return priority;
    }

    @Override
    public UUID getConnectionOwner() {
        return playerUUID;
    }

    @Override
    public boolean canAccess(EntityPlayer player) {
        if (!network.isInvalid()) {
            if (EntityPlayer.getUUID(player.getGameProfile()).equals(playerUUID)) {
                return true;
            }
            return network.getMemberPermission(player).canAccess();
        }
        return true;
    }

    @Override
    public long getLogicLimit() {
        return disableLimit ? size.transferLimit : limit;
    }

    @Override
    public long getRawLimit() {
        return limit;
    }

    @Override
    public long getMaxTransferLimit() {
        return size.transferLimit;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isChunkLoaded() {
        return !isInvalid();
    }

    @Override
    public boolean isForcedLoading() {
        return chunkLoading;
    }

    @Override
    public void connect(IFluxNetwork network) {
        this.network = network;
        this.networkID = network.getNetworkID();
        getTransferHandler().reset();
        markForUpdate();
    }

    @Override
    public void disconnect(IFluxNetwork network) {
        if (network.getNetworkID() == getNetworkID()) {
            this.network = FluxNetworkInvalid.instance;
            this.networkID = -1;
            getTransferHandler().reset();
            markForUpdate();
        }
    }

    @Override
    public ITransferHandler getTransferHandler() {
        return transferHandler;
    }

    @Override
    public World getFluxWorld() {
        return world;
    }

    @Override
    public BlockPos getFluxPos() {
        return pos;
    }

    @Override
    public Coord4D getCoords() {
        if (coord4D == null) {
            coord4D = new Coord4D(this);
        }
        return coord4D;
    }

    @Override
    public int getFolderID() {
        return folderID;
    }

    @Override
    public String getCustomName() {
        if (isCustomName) {
            return customName;
        }

        if (machineName != null) {
            return machineName;
        }

        String connectedMachineStr = connectedMachine.get();
        if (connectedMachineStr.isEmpty()) {
            return customName;
        }
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(connectedMachineStr));
        if (machine != null) {
            var name = machine.getRawLocalizedName();
            if (name.length() > 22) {
                name = name.substring(0, 22);
                name = name + "...";
            }
            this.machineName = name;
            return name;
        }

        return customName;
    }

    @Override
    public boolean getDisableLimit() {
        return disableLimit;
    }

    @Override
    public boolean getSurgeMode() {
        return surgeMode;
    }

    @Override
    public long getTransferBuffer() {
        return getTransferHandler().getBuffer();
    }

    @Override
    public long getTransferChange() {
        return getTransferHandler().getChange();
    }

    @Override
    public ItemStack getDisplayStack() {
        return displayStack;
    }

    @Override
    public int getNetworkID() {
        return networkID;
    }

    @Override
    public IFluxNetwork getNetwork() {
        return network;
    }

    @Override
    public void open(EntityPlayer player) {
        if (!world.isRemote) {
            playerUsing.add(player);
        }
    }

    @Override
    public void close(EntityPlayer player) {
        if (!world.isRemote) {
            playerUsing.remove(player);
        }
    }

    @Override
    public void onMachineEvent(MachineEvent event) {
        if (!(event instanceof MachineStructureUpdateEvent updateEvent)) {
            return;
        }

        var controller = updateEvent.getController();
        if (connectedMachine.get().equals(controller.getFormedMachineName())) {
            return;
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            var state = controller.getWorld().getBlockState(controller.getPos());
            displayStack = new ItemStack(Item.getItemFromBlock(state.getBlock()));
            connectedMachine.set(controller.getFormedMachineName());
            markForUpdate();
        });
    }

    @Override
    public NBTTagCompound copyConfiguration(NBTTagCompound config) {
        return FluxUtils.copyConfiguration(this, config);
    }

    @Override
    public void pasteConfiguration(NBTTagCompound config) {
        FluxUtils.pasteConfiguration(this, config);
    }

    public boolean isPlayerUsing() {
        return !playerUsing.isEmpty();
    }

    @Override
    public void setCustomName(String customName) {
        this.customName = customName;
        this.isCustomName = true;
        markForUpdate();
    }

    @Override
    public void setForcedLoading(boolean chunkLoading) {
        this.chunkLoading = chunkLoading;
    }

    @Override
    public void setDisableLimit(boolean disableLimit) {
        this.disableLimit = disableLimit;
    }

    @Override
    public void setRawLimit(long limit) {
        this.limit = limit;
    }

    @Override
    public void setSurgeMode(boolean surgeMode) {
        this.surgeMode = surgeMode;
    }

    @Override
    public void setRawPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public void setConnectionOwner(UUID owner) {
        this.playerUUID = owner;
    }

    @Override
    public void notifyFluxUpdate() {
        markForUpdate();
    }

    public abstract long computeRequest();

    private class TransferHandler implements ITransferHandler {

        private long transferChange;
        private long lastCycleChange;

        private long maxTransfer = Math.min(size.transferLimit, getLogicLimit());

        @Override
        public void onCycleStart() {
        }

        @Override
        public void onCycleEnd() {
            lastCycleChange = transferChange;
            transferChange = 0;
            maxTransfer = Math.min(size.transferLimit, getLogicLimit());
            markNoUpdate();
        }

        @Override
        public long getBuffer() {
            return energy.get();
        }

        @Override
        public long getRequest() {
            return Math.min(maxTransfer, computeRequest());
        }

        @Override
        public long getChange() {
            return lastCycleChange;
        }

        @Override
        public void addToBuffer(long amount) {
            long toAdd = Math.min(amount, getRemainingCapacity());
            if (toAdd <= 0) {
                return;
            }

            energy.addAndGet(toAdd);
            transferChange -= toAdd;
            updateMaxTransfer(toAdd);
        }

        @Override
        public long removeFromBuffer(long amount) {
            long toRemove = Math.min(Math.min(amount, maxTransfer), getCurrentEnergy());
            if (toRemove <= 0) {
                return 0;
            }

            energy.addAndGet(-toRemove);
            transferChange += toRemove;
            updateMaxTransfer(toRemove);
            return toRemove;
        }

        @Override
        public long receiveFromSupplier(long l, @Nonnull EnumFacing enumFacing, boolean b) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeCustomNBT(NBTTagCompound compound, NBTType nbtType) {
            compound.setLong("lastCycleChange", lastCycleChange);
        }

        @Override
        public void readCustomNBT(NBTTagCompound compound, NBTType nbtType) {
            this.lastCycleChange = compound.getLong("lastCycleChange");
        }

        @Override
        public void updateTransfers(EnumFacing... enumFacings) {
        }

        @Override
        public void reset() {
            transferChange = 0;
            lastCycleChange = 0;
        }

        private void updateMaxTransfer(long transferred) {
            maxTransfer = Math.min(0, maxTransfer - transferred);
        }
    }
}
