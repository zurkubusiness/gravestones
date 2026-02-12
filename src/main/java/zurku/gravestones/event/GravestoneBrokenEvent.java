package zurku.gravestones.event;

import com.hypixel.hytale.event.IEvent;
import java.util.UUID;

/**
 * Fired when a gravestone block is broken (by any means).
 */
public class GravestoneBrokenEvent implements IEvent<Void> {

    private final UUID breakerUuid;
    private final UUID ownerUuid;
    private final int x;
    private final int y;
    private final int z;
    private final String worldName;

    public GravestoneBrokenEvent(UUID breakerUuid, UUID ownerUuid, int x, int y, int z, String worldName) {
        this.breakerUuid = breakerUuid;
        this.ownerUuid = ownerUuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
    }

    public UUID getBreakerUuid() {
        return breakerUuid;
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
