package zurku.gravestones.event;

import com.hypixel.hytale.event.IEvent;
import java.util.UUID;

/**
 * Fired after a player collects items from a gravestone (right-click interaction).
 */
public class GravestoneCollectedEvent implements IEvent<Void> {

    private final UUID collectorUuid;
    private final UUID ownerUuid;
    private final int x;
    private final int y;
    private final int z;
    private final String worldName;

    public GravestoneCollectedEvent(UUID collectorUuid, UUID ownerUuid, int x, int y, int z, String worldName) {
        this.collectorUuid = collectorUuid;
        this.ownerUuid = ownerUuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
    }

    public UUID getCollectorUuid() {
        return collectorUuid;
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
}
