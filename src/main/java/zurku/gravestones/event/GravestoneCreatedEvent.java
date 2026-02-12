package zurku.gravestones.event;

import com.hypixel.hytale.event.IEvent;
import java.util.UUID;

/**
 * Fired after a gravestone has been successfully created and placed in the world.
 */
public class GravestoneCreatedEvent implements IEvent<Void> {

    private final UUID ownerUuid;
    private final int x;
    private final int y;
    private final int z;
    private final String worldName;

    public GravestoneCreatedEvent(UUID ownerUuid, int x, int y, int z, String worldName) {
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
}
