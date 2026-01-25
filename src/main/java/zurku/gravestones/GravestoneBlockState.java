package zurku.gravestones;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.BreakValidatedBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.DestroyableBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import java.util.UUID;
import java.util.function.Predicate;

@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public class GravestoneBlockState extends ItemContainerState 
        implements ItemContainerBlockState, DestroyableBlockState, BreakValidatedBlockState {

    private static GravestoneSettings settings;
    
    public static void setSettings(GravestoneSettings s) {
        settings = s;
    }

    public static final Codec<GravestoneBlockState> CODEC = BuilderCodec.builder(
            GravestoneBlockState.class, GravestoneBlockState::new, BlockState.BASE_CODEC)
        .append(new KeyedCodec<>("OwnerUUID", Codec.UUID_BINARY),
            (state, uuid) -> state.ownerUUID = uuid, state -> state.ownerUUID).add()
        .append(new KeyedCodec<>("OwnerName", Codec.STRING),
            (state, name) -> state.ownerName = name, state -> state.ownerName).add()
        .append(new KeyedCodec<>("DeathTime", Codec.LONG),
            (state, time) -> state.deathTime = time, state -> state.deathTime).add()
        .append(new KeyedCodec<>("DynamicCapacity", Codec.SHORT),
            (state, cap) -> state.dynamicCapacity = cap, state -> state.dynamicCapacity).add()
        .append(new KeyedCodec<>("ItemContainer", SimpleItemContainer.CODEC),
            (state, container) -> state.itemContainer = container, state -> state.itemContainer).add()
        .build();

    protected UUID ownerUUID;
    protected String ownerName;
    protected long deathTime;
    protected short dynamicCapacity;
    protected SimpleItemContainer itemContainer;

    public GravestoneBlockState() {
        this.deathTime = System.currentTimeMillis();
    }

    @Override
    public boolean initialize(BlockType blockType) {
        short capacity = dynamicCapacity > 0 ? dynamicCapacity : 63;
        
        StateData stateData = blockType.getState();
        if (stateData instanceof GravestoneStateData) {
            GravestoneStateData data = (GravestoneStateData) stateData;
            if (dynamicCapacity <= 0) {
                capacity = data.getCapacity();
            }
        }

        if (itemContainer == null) {
            itemContainer = new SimpleItemContainer(capacity);
        } else if (itemContainer.getCapacity() < capacity) {
            SimpleItemContainer newContainer = new SimpleItemContainer(capacity);
            for (short i = 0; i < itemContainer.getCapacity(); i++) {
                ItemStack item = itemContainer.getItemStack(i);
                if (item != null && !ItemStack.isEmpty(item)) {
                    newContainer.addItemStackToSlot(i, item);
                }
            }
            itemContainer = newContainer;
        }

        itemContainer.setGlobalFilter(FilterType.ALLOW_OUTPUT_ONLY);
        itemContainer.registerChangeEvent(com.hypixel.hytale.event.EventPriority.LAST, this::onItemChange);
        return true;
    }

    @Override
    public void onItemChange(ItemContainer.ItemContainerChangeEvent event) {
        if (isEmpty()) {
            destroyBlock();
        }
        markNeedsSave();
    }

    private boolean isEmpty() {
        if (itemContainer == null) return true;
        if (itemContainer.isEmpty()) return true;
        return itemContainer.countItemStacks((Predicate<ItemStack>) item -> !ItemStack.isEmpty(item)) == 0;
    }

    private void destroyBlock() {
        WorldChunk chunk = getChunk();
        if (chunk == null) return;
        World world = chunk.getWorld();
        var pos = getBlockPosition();
        itemContainer = null;
        world.execute(() -> world.breakBlock(pos.getX(), pos.getY(), pos.getZ(), 0));
    }
    
    @Override
    public void onDestroy() {
        if (itemContainer != null && !itemContainer.isEmpty()) {
            WorldChunk chunk = getChunk();
            if (chunk != null) {
                World world = chunk.getWorld();
                if (world != null) {
                    var droppedItems = itemContainer.dropAllItemStacks();
                    if (!droppedItems.isEmpty()) {
                        var pos = getBlockPosition().toVector3d().add(0.5, 0.5, 0.5);
                        world.execute(() -> {
                            var store = world.getEntityStore().getStore();
                            var holders = com.hypixel.hytale.server.core.modules.entity.item.ItemComponent.generateItemDrops(
                                store, droppedItems, pos, com.hypixel.hytale.math.vector.Vector3f.ZERO);
                            if (holders.length > 0) {
                                store.addEntities(holders, com.hypixel.hytale.component.AddReason.SPAWN);
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public ItemContainer getItemContainer() {
        return itemContainer;
    }

    public void setItemContainer(ItemContainer container) {
        if (container instanceof SimpleItemContainer) {
            this.itemContainer = (SimpleItemContainer) container;
        }
    }

    @Override
    public boolean canOpen(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        if (settings != null && settings.isOwnerProtection() && ownerUUID != null) {
            UUIDComponent uuidComp = accessor.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                UUID playerUuid = uuidComp.getUuid();
                if (!ownerUUID.equals(playerUuid)) {
                    return PermissionsModule.get().hasPermission(playerUuid, "gravestones.access_any");
                }
            }
        }
        return true;
    }

    @Override
    public boolean canDestroy(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        if (settings != null && settings.isOwnerProtection() && ownerUUID != null) {
            UUIDComponent uuidComp = accessor.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                UUID playerUuid = uuidComp.getUuid();
                if (!ownerUUID.equals(playerUuid)) {
                    return PermissionsModule.get().hasPermission(playerUuid, "gravestones.destroy_any");
                }
            }
        }
        return true;
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        markNeedsSave();
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getDeathTime() {
        return deathTime;
    }

    
    public void setDynamicCapacity(short capacity) {
        this.dynamicCapacity = capacity;
        
        SimpleItemContainer newContainer = new SimpleItemContainer(capacity);
        
        if (itemContainer != null && !itemContainer.isEmpty()) {
            for (short i = 0; i < Math.min(itemContainer.getCapacity(), capacity); i++) {
                ItemStack item = itemContainer.getItemStack(i);
                if (item != null && !ItemStack.isEmpty(item)) {
                    newContainer.addItemStackToSlot(i, item);
                }
            }
        }
        
        itemContainer = newContainer;
        markNeedsSave();
    }
    
    
    public void finalizeContainer() {
        if (itemContainer != null) {
            itemContainer.setGlobalFilter(FilterType.ALLOW_OUTPUT_ONLY);
            itemContainer.registerChangeEvent(com.hypixel.hytale.event.EventPriority.LAST, this::onItemChange);
        }
    }

    public static class GravestoneStateData extends StateData {
        public static final BuilderCodec<GravestoneStateData> CODEC = BuilderCodec.builder(
                GravestoneStateData.class, GravestoneStateData::new, StateData.DEFAULT_CODEC)
            .appendInherited(new KeyedCodec<>("Capacity", Codec.INTEGER),
                (data, cap) -> data.capacity = cap.shortValue(),
                data -> Integer.valueOf(data.capacity),
                (data, other) -> data.capacity = other.capacity).add()
            .build();

        private short capacity = 63;

        protected GravestoneStateData() {}

        public short getCapacity() {
            return capacity;
        }

        @Override
        public String toString() {
            return capacity + super.toString();
        }
    }
}
