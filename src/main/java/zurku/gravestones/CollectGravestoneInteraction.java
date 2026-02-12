package zurku.gravestones;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.event.IEventDispatcher;
import zurku.gravestones.event.GravestoneCollectedEvent;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"rawtypes", "unchecked", "removal"})
public class CollectGravestoneInteraction extends SimpleBlockInteraction {

    private static GravestoneManager manager;

    public static final BuilderCodec<CollectGravestoneInteraction> CODEC = BuilderCodec.builder(
        CollectGravestoneInteraction.class,
        CollectGravestoneInteraction::new,
        SimpleBlockInteraction.CODEC
    ).build();

    public static final CollectGravestoneInteraction INSTANCE = new CollectGravestoneInteraction("Collect_Gravestone");

    public static final RootInteraction ROOT = new RootInteraction(
        INSTANCE.getId(),
        new String[] { INSTANCE.getId() }
    );

    public static void setManager(GravestoneManager mgr) {
        manager = mgr;
    }

    public CollectGravestoneInteraction() {
        super();
    }

    public CollectGravestoneInteraction(String id) {
        super(id);
    }

    @Override
    protected void interactWithBlock(
            World world,
            CommandBuffer<EntityStore> buffer,
            InteractionType type,
            InteractionContext ctx,
            ItemStack held,
            Vector3i pos,
            CooldownHandler cooldown) {

        Ref ref = ctx.getEntity();
        Store store = ref.getStore();
        Player player = buffer.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (manager != null) {
            manager.registerWorld(world);
        }

        // External access checker (before built-in owner check)
        boolean skipOwnerCheck = false;
        if (manager != null && manager.getAccessChecker() != null) {
            UUID owner = manager.getGravestoneOwner(pos.x, pos.y, pos.z);
            UUIDComponent uuidComp = (UUIDComponent) store.getComponent(ref, UUIDComponent.getComponentType());
            UUID accessorUuid = uuidComp != null ? uuidComp.getUuid() : null;
            if (accessorUuid != null) {
                GravestoneAccessChecker.AccessResult result = manager.getAccessChecker().canAccess(
                        accessorUuid, owner, pos.x, pos.y, pos.z, world.getName());
                if (result == GravestoneAccessChecker.AccessResult.DENY) return;
                if (result == GravestoneAccessChecker.AccessResult.ALLOW) skipOwnerCheck = true;
            }
        }

        if (!skipOwnerCheck && manager != null && manager.getSettings().isOwnerProtection()) {
            UUID owner = manager.getGravestoneOwner(pos.x, pos.y, pos.z);
            if (owner != null) {
                UUIDComponent uuidComp = (UUIDComponent) store.getComponent(ref, UUIDComponent.getComponentType());
                if (uuidComp == null || !owner.equals(uuidComp.getUuid())) {
                    return;
                }
            }
        }

        BlockState state = BlockStateUtil.getBlockState(world, pos.x, pos.y, pos.z);
        if (!(state instanceof ItemContainerState)) return;

        ItemContainerState containerState = (ItemContainerState) state;
        if (!containerState.isAllowViewing() || !containerState.canOpen(ref, buffer)) return;

        CombinedItemContainer playerInv = player.getInventory().getCombinedEverything();
        ItemContainer graveInv = containerState.getItemContainer();

        for (short i = 0; i < graveInv.getCapacity(); i++) {
            ItemStack item = graveInv.getItemStack(i);
            if (item != null && !item.isEmpty()) {
                ItemStack remainder = playerInv.addItemStack(item).getRemainder();
                graveInv.setItemStackForSlot(i, remainder);
            }
        }

        if (!graveInv.isEmpty()) {
            world.execute(() -> {
                HeadRotation rot = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
                Vector3f facing = rot != null ? rot.getRotation() : new Vector3f();
                List<ItemStack> remaining = graveInv.dropAllItemStacks();
                Vector3d dropPos = pos.clone().add(0, 1, 0).toVector3d();

                Holder[] drops = ItemComponent.generateItemDrops(store, remaining, dropPos, facing);
                for (Holder drop : drops) {
                    world.getEntityStore().getStore().addEntity(drop, AddReason.SPAWN);
                }
            });
        }

        // Fire collected event
        try {
            UUID collectorUuid = null;
            UUID ownerUuid = manager != null ? manager.getGravestoneOwner(pos.x, pos.y, pos.z) : null;
            UUIDComponent collectorComp = (UUIDComponent) store.getComponent(ref, UUIDComponent.getComponentType());
            if (collectorComp != null) collectorUuid = collectorComp.getUuid();
            if (collectorUuid != null) {
                final UUID cUuid = collectorUuid;
                final UUID oUuid = ownerUuid;
                IEventDispatcher<GravestoneCollectedEvent, GravestoneCollectedEvent> dispatcher =
                    HytaleServer.get().getEventBus().dispatchFor(GravestoneCollectedEvent.class);
                if (dispatcher.hasListener()) {
                    dispatcher.dispatch(new GravestoneCollectedEvent(cUuid, oUuid, pos.x, pos.y, pos.z, world.getName()));
                }
            }
        } catch (Exception ignored) {}

        world.execute(() -> world.breakBlock(pos.x, pos.y, pos.z, 0));
    }

    @Override
    protected void simulateInteractWithBlock(
            InteractionType type,
            InteractionContext ctx,
            ItemStack held,
            World world,
            Vector3i pos) {
    }
}
