package zurku.gravestones;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.BreakBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

@SuppressWarnings({"rawtypes", "unchecked"})
public class BreakGravestoneInteraction extends BreakBlockInteraction {

    private static GravestoneManager manager;

    public static final BuilderCodec<BreakGravestoneInteraction> CODEC = BuilderCodec.builder(
        BreakGravestoneInteraction.class,
        BreakGravestoneInteraction::new,
        BreakBlockInteraction.CODEC
    ).build();

    public static final BreakGravestoneInteraction INSTANCE = new BreakGravestoneInteraction("Break_Gravestone");

    public static final RootInteraction ROOT = new RootInteraction(
        INSTANCE.getId(),
        new String[] { INSTANCE.getId() }
    );

    public static void setManager(GravestoneManager mgr) {
        manager = mgr;
    }

    public BreakGravestoneInteraction() {
        super();
    }

    public BreakGravestoneInteraction(String id) {
        super();
        try {
            java.lang.reflect.Field idField = Interaction.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(this, id);
        } catch (Exception ignored) {}
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

        if (manager != null) {
            manager.registerWorld(world);
        }

        // External access checker (before built-in owner check)
        boolean skipOwnerCheck = false;
        if (manager != null && manager.getAccessChecker() != null) {
            UUID owner = manager.getGravestoneOwner(pos.x, pos.y, pos.z);
            Ref ref = ctx.getEntity();
            Store store = ref.getStore();
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
                Ref ref = ctx.getEntity();
                Store store = ref.getStore();
                UUIDComponent uuidComp = (UUIDComponent) store.getComponent(ref, UUIDComponent.getComponentType());
                if (uuidComp == null || !owner.equals(uuidComp.getUuid())) {
                    return;
                }
            }
        }
        super.interactWithBlock(world, buffer, type, ctx, held, pos, cooldown);
    }
}
