package zurku.gravestones.event;

import com.hypixel.hytale.event.ICancellable;
import com.hypixel.hytale.event.IEvent;
import java.util.UUID;

/**
 * Fired before a gravestone is placed in the world.
 * Cancel this event to prevent gravestone creation (items will drop normally per vanilla behavior).
 */
public class GravestonePreCreateEvent implements IEvent<Void>, ICancellable {

    private final UUID ownerUuid;
    private final int x;
    private final int y;
    private final int z;
    private final String worldName;
    private boolean cancelled;

    public GravestonePreCreateEvent(UUID ownerUuid, int x, int y, int z, String worldName) {
        this.ownerUuid = ownerUuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getWorldName() {
        return worldName;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
