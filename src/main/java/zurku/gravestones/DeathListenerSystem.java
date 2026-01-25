package zurku.gravestones;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import javax.annotation.Nonnull;

@SuppressWarnings("unchecked")
public class DeathListenerSystem extends DeathSystems.OnDeathSystem {

    private final GravestonePlugin plugin;
    private final HytaleLogger logger;

    public DeathListenerSystem(GravestonePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent death,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            if (player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) return;

            death.setDisplayDataOnDeathScreen(true);

            World world = player.getWorld();
            if (world == null) return;

            DeathConfig config = world.getDeathConfig();

            DeathConfig.ItemsLossMode mode = config.getItemsLossMode();
            if (mode == DeathConfig.ItemsLossMode.NONE) return;

            Inventory inventory = player.getInventory();
            CombinedItemContainer combined = inventory.getCombinedEverything();

            double durLoss = config.getItemsDurabilityLossPercentage();
            if (durLoss > 0) {
                double ratio = durLoss / 100.0;
                boolean armorBroken = false;

                for (short i = 0; i < combined.getCapacity(); i++) {
                    ItemStack item = combined.getItemStack(i);
                    if (item == null || ItemStack.isEmpty(item) || item.isBroken()) continue;

                    double loss = item.getMaxDurability() * ratio;
                    ItemStack damaged = item.withIncreasedDurability(-loss);
                    ItemStackSlotTransaction tx = combined.replaceItemStackInSlot(i, item, damaged);

                    ItemStack afterSlot = tx.getSlotAfter();
                    if (afterSlot != null && afterSlot.isBroken() && item.getItem().getArmor() != null) {
                        armorBroken = true;
                    }
                }

                if (armorBroken) {
                    player.getStatModifiersManager().setRecalculate(true);
                }
            }

            List<ItemStack> items = null;
            double lossPercent = config.getItemsAmountLossPercentage();

            switch (mode) {
                case ALL:
                    items = inventory.dropAllItemStacks();
                    break;

                case CONFIGURED:
                    if (lossPercent > 0) {
                        double ratio = lossPercent / 100.0;
                        items = new ObjectArrayList<>();

                        for (short i = 0; i < combined.getCapacity(); i++) {
                            ItemStack item = combined.getItemStack(i);
                            if (item == null || ItemStack.isEmpty(item) || !item.getItem().dropsOnDeath()) continue;

                            int take = Math.max(1, MathUtil.floor(item.getQuantity() * ratio));
                            items.add(item.withQuantity(take));

                            int keep = item.getQuantity() - take;
                            if (keep > 0) {
                                combined.replaceItemStackInSlot(i, item, item.withQuantity(keep));
                            } else {
                                combined.removeItemStackFromSlot(i);
                            }
                        }
                    }
                    break;

                default:
                    break;
            }

            if (items != null && !items.isEmpty()) {
                plugin.getGravestoneManager().onPlayerDeath(player, store, ref, items.toArray(new ItemStack[0]));
                death.setItemsLostOnDeath(items);
            }

        } catch (Exception e) {
            logger.atSevere().log("[Gravestones] Death processing error: " + e.getMessage());
        }
    }
}
