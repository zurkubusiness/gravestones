package zurku.gravestones;

import java.util.UUID;

public class GravestoneData {

    private UUID playerId;
    private int x;
    private int y;
    private int z;
    private String worldName;
    private long createdTime;

    public GravestoneData() {}

    public GravestoneData(UUID playerId, int x, int y, int z, String worldName) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
        this.createdTime = System.currentTimeMillis();
    }

    public UUID getPlayerId() { return playerId; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getWorldName() { return worldName; }
    public long getCreatedTime() { return createdTime; }

    public String getLocationKey() {
        return worldName + ":" + x + "," + y + "," + z;
    }
}
