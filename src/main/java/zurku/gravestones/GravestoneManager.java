package zurku.gravestones;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.event.IEventDispatcher;
import zurku.gravestones.event.GravestonePreCreateEvent;
import zurku.gravestones.event.GravestoneCreatedEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GravestoneManager {

    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY = 200L;
    private static final String DATA_PATH = "plugins/Gravestones/gravestones.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final GravestoneSettings settings;
    private final HytaleLogger logger;
    private final Map<UUID, List<GravestoneData>> playerGraves;
    private final Map<String, GravestoneData> locationIndex;
    private final Map<String, World> worldCache;
    private final List<GravestoneData> pendingRemovals;
    private final ScheduledExecutorService executor;
    private World currentWorld;
    private GravestoneAccessChecker accessChecker;

    public GravestoneManager(GravestonePlugin plugin, GravestoneSettings settings) {
        this.settings = settings;
        this.logger = plugin.getLogger();
        this.playerGraves = new ConcurrentHashMap<>();
        this.locationIndex = new ConcurrentHashMap<>();
        this.worldCache = new ConcurrentHashMap<>();
        this.pendingRemovals = new CopyOnWriteArrayList<>();
        this.executor = Executors.newSingleThreadScheduledExecutor();
        
        load();
        executor.scheduleAtFixedRate(this::checkDespawnTimers, 60, 60, TimeUnit.SECONDS);
    }

    public void shutdown() {
        save();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void registerWorld(World world) {
        if (world != null) {
            worldCache.put(world.getName(), world);
            currentWorld = world;
            processPendingRemovals(world);
        }
    }

    private void processPendingRemovals(World world) {
        if (pendingRemovals.isEmpty()) return;
        
        List<GravestoneData> processed = new ArrayList<>();
        for (GravestoneData data : pendingRemovals) {
            if (data.getWorldName().equals(world.getName())) {
                int x = data.getX(), y = data.getY(), z = data.getZ();
                world.execute(() -> world.breakBlock(x, y, z, 0));
                logger.atInfo().log("[Gravestones] Despawned queued gravestone at (" + x + ", " + y + ", " + z + ")");
                processed.add(data);
            }
        }
        pendingRemovals.removeAll(processed);
    }

    private void load() {
        File file = new File(DATA_PATH);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<GravestoneData>>(){}.getType();
            List<GravestoneData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                for (GravestoneData data : loaded) {
                    playerGraves.computeIfAbsent(data.getPlayerId(), k -> new ArrayList<>()).add(data);
                    locationIndex.put(data.getLocationKey(), data);
                }
                logger.atInfo().log("[Gravestones] Loaded " + loaded.size() + " gravestones from disk");
            }
        } catch (Exception e) {
            logger.atWarning().log("[Gravestones] Failed to load gravestones: " + e.getMessage());
        }
    }

    private void save() {
        try {
            File file = new File(DATA_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            List<GravestoneData> allGraves = new ArrayList<>(locationIndex.values());
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(allGraves, writer);
            }
        } catch (Exception e) {
            logger.atWarning().log("[Gravestones] Failed to save gravestones: " + e.getMessage());
        }
    }

    public GravestoneSettings getSettings() {
        return settings;
    }

    public void setAccessChecker(GravestoneAccessChecker checker) {
        this.accessChecker = checker;
        GravestoneBlockState.setAccessChecker(checker);
    }

    public GravestoneAccessChecker getAccessChecker() {
        return accessChecker;
    }

    /**
     * Returns an unmodifiable copy of the given player's gravestones, or an empty list.
     */
    public List<GravestoneData> getPlayerGravestones(UUID playerId) {
        List<GravestoneData> list = playerGraves.get(playerId);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /**
     * Returns the gravestone data at the exact position, or null if none exists.
     */
    public GravestoneData getGravestoneAt(int x, int y, int z, String worldName) {
        String key = worldName + ":" + x + "," + y + "," + z;
        return locationIndex.get(key);
    }

    /**
     * Returns the count of active gravestones for the given player.
     */
    public int getGravestoneCount(UUID playerId) {
        List<GravestoneData> list = playerGraves.get(playerId);
        return list != null ? list.size() : 0;
    }

    /**
     * Programmatically removes a gravestone: deletes data, breaks the block in-world, and saves.
     *
     * @return true if a gravestone was found and removed, false otherwise
     */
    public boolean destroyGravestone(int x, int y, int z, String worldName) {
        String key = worldName + ":" + x + "," + y + "," + z;
        GravestoneData data = locationIndex.remove(key);
        if (data == null) return false;

        List<GravestoneData> list = playerGraves.get(data.getPlayerId());
        if (list != null) {
            list.removeIf(g -> g.getLocationKey().equals(key));
        }
        save();

        World world = worldCache.get(worldName);
        if (world == null) world = currentWorld;
        if (world != null && world.getName().equals(worldName)) {
            final World targetWorld = world;
            targetWorld.execute(() -> targetWorld.breakBlock(x, y, z, 0));
        }
        return true;
    }

    public UUID getGravestoneOwner(int x, int y, int z) {
        String suffix = ":" + x + "," + y + "," + z;
        for (Map.Entry<String, GravestoneData> entry : locationIndex.entrySet()) {
            if (entry.getKey().endsWith(suffix)) {
                return entry.getValue().getPlayerId();
            }
        }
        return null;
    }

    private void checkDespawnTimers() {
        int despawnMinutes = settings.getDespawnMinutes();
        if (despawnMinutes <= 0) return;

        long now = System.currentTimeMillis();
        long maxAge = despawnMinutes * 60L * 1000L;

        List<GravestoneData> toRemove = new ArrayList<>();
        for (GravestoneData data : locationIndex.values()) {
            if (now - data.getCreatedTime() > maxAge) {
                toRemove.add(data);
            }
        }

        for (GravestoneData data : toRemove) {
            removeGravestoneData(data);
            int x = data.getX(), y = data.getY(), z = data.getZ();
            String worldName = data.getWorldName();
            
            World world = worldCache.get(worldName);
            if (world == null) world = currentWorld;
            
            if (world != null) {
                final World targetWorld = world;
                targetWorld.execute(() -> targetWorld.breakBlock(x, y, z, 0));
                logger.atInfo().log("[Gravestones] Despawned gravestone at (" + x + ", " + y + ", " + z + ") due to timer");
            } else {
                pendingRemovals.add(data);
                logger.atInfo().log("[Gravestones] Queued gravestone at (" + x + ", " + y + ", " + z + ") for removal (world not loaded)");
            }
        }
        
        if (!toRemove.isEmpty()) {
            save();
        }
        
        processPendingRemovalsFromCache();
    }
    
    private void processPendingRemovalsFromCache() {
        if (pendingRemovals.isEmpty()) return;
        
        List<GravestoneData> processed = new ArrayList<>();
        for (GravestoneData data : pendingRemovals) {
            World world = worldCache.get(data.getWorldName());
            if (world == null) world = currentWorld;
            
            if (world != null && world.getName().equals(data.getWorldName())) {
                int x = data.getX(), y = data.getY(), z = data.getZ();
                final World targetWorld = world;
                targetWorld.execute(() -> targetWorld.breakBlock(x, y, z, 0));
                logger.atInfo().log("[Gravestones] Despawned queued gravestone at (" + x + ", " + y + ", " + z + ")");
                processed.add(data);
            }
        }
        pendingRemovals.removeAll(processed);
    }

    private void removeGravestoneData(GravestoneData data) {
        locationIndex.remove(data.getLocationKey());
        List<GravestoneData> list = playerGraves.get(data.getPlayerId());
        if (list != null) {
            list.removeIf(g -> g.getLocationKey().equals(data.getLocationKey()));
        }
    }

    private void enforcePlayerLimit(UUID playerId, World world) {
        int limit = settings.getMaxPerPlayer();
        if (limit <= 0) return;

        List<GravestoneData> list = playerGraves.get(playerId);
        if (list == null || list.size() <= limit) return;

        list.sort((a, b) -> Long.compare(a.getCreatedTime(), b.getCreatedTime()));
        boolean removed = false;
        while (list.size() > limit) {
            GravestoneData oldest = list.remove(0);
            locationIndex.remove(oldest.getLocationKey());
            int x = oldest.getX(), y = oldest.getY(), z = oldest.getZ();
            world.execute(() -> world.breakBlock(x, y, z, 0));
            logger.atInfo().log("[Gravestones] Removed oldest gravestone at (" + x + ", " + y + ", " + z + ") due to limit");
            removed = true;
        }
        
        if (removed) {
            save();
        }
    }

    public void removeGravestoneAtPosition(int x, int y, int z) {
        String suffix = ":" + x + "," + y + "," + z;
        String key = null;
        
        for (String k : locationIndex.keySet()) {
            if (k.endsWith(suffix)) {
                key = k;
                break;
            }
        }
        
        if (key != null) {
            GravestoneData data = locationIndex.remove(key);
            if (data != null) {
                List<GravestoneData> list = playerGraves.get(data.getPlayerId());
                if (list != null) {
                    String finalKey = key;
                    list.removeIf(g -> g.getLocationKey().equals(finalKey));
                }
                save();
            }
        }
    }

    public void onPlayerDeath(Player player, Store<EntityStore> store, Ref<EntityStore> ref, ItemStack[] items) {
        try {
            UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp == null) return;

            UUID playerId = uuidComp.getUuid();
            String playerName = player.getDisplayName();
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) return;

            Vector3d pos = transform.getPosition();
            int x = (int) pos.getX();
            int y = (int) pos.getY();
            int z = (int) pos.getZ();

            World world = player.getWorld();
            if (world == null) return;
            
            registerWorld(world);

            List<ItemStack> itemList = new ArrayList<>();
            if (items != null) {
                for (ItemStack item : items) {
                    if (item != null && !item.isEmpty()) {
                        itemList.add(item);
                    }
                }
            }
            if (itemList.isEmpty()) return;

            String worldName = world.getName();

            // Fire pre-create event (cancellable)
            try {
                IEventDispatcher<GravestonePreCreateEvent, GravestonePreCreateEvent> preDispatcher =
                    HytaleServer.get().getEventBus().dispatchFor(GravestonePreCreateEvent.class);
                if (preDispatcher.hasListener()) {
                    GravestonePreCreateEvent preEvent = preDispatcher.dispatch(
                        new GravestonePreCreateEvent(playerId, x, y, z, worldName));
                    if (preEvent != null && preEvent.isCancelled()) {
                        return;
                    }
                }
            } catch (Exception e) {
                logger.atWarning().log("[Gravestones] Pre-create event error: " + e.getMessage());
            }

            GravestoneData data = new GravestoneData(playerId, x, y, z, worldName);
            playerGraves.computeIfAbsent(playerId, k -> new ArrayList<>()).add(data);
            locationIndex.put(data.getLocationKey(), data);
            save();

            enforcePlayerLimit(playerId, world);

            logger.atInfo().log("[Gravestones] Created for " + playerName + " at (" + x + ", " + y + ", " + z + ")");

            // Fire created event
            try {
                IEventDispatcher<GravestoneCreatedEvent, GravestoneCreatedEvent> createdDispatcher =
                    HytaleServer.get().getEventBus().dispatchFor(GravestoneCreatedEvent.class);
                if (createdDispatcher.hasListener()) {
                    createdDispatcher.dispatch(new GravestoneCreatedEvent(playerId, x, y, z, worldName));
                }
            } catch (Exception e) {
                logger.atWarning().log("[Gravestones] Created event error: " + e.getMessage());
            }

            List<ItemStack> finalItems = new ArrayList<>(itemList);
            executor.schedule(() -> world.execute(() -> placeGravestone(world, x, y, z, finalItems)), 100L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.atSevere().log("[Gravestones] Death processing error: " + e.getMessage());
        }
    }

    private void placeGravestone(World world, int x, int y, int z, List<ItemStack> items) {
        try {
            long chunkKey = ((long) (x >> 4) << 32) | ((long) (z >> 4) & 0xFFFFFFFFL);
            WorldChunk chunk = world.getChunkIfLoaded(chunkKey);

            if (chunk == null) {
                world.getChunkAsync(chunkKey).thenAccept(c -> {
                    if (c != null) {
                        world.execute(() -> doPlacement(world, c, x, y, z, items));
                    }
                });
            } else {
                doPlacement(world, chunk, x, y, z, items);
            }
        } catch (Exception e) {
            logger.atSevere().log("[Gravestones] Placement error: " + e.getMessage());
        }
    }

    private void doPlacement(World world, WorldChunk chunk, int x, int y, int z, List<ItemStack> items) {
        try {
            chunk.setBlock(x, y, z, 0);
            world.setBlock(x, y, z, settings.getGravestoneBlockId());
            executor.schedule(() -> world.execute(() -> fillContainer(world, x, y, z, items, 0)), 100L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.atSevere().log("[Gravestones] Block placement error: " + e.getMessage());
        }
    }

    @SuppressWarnings("removal")
    private void fillContainer(World world, int x, int y, int z, List<ItemStack> items, int attempt) {
        try {
            BlockState state = BlockStateUtil.getBlockState(world, x, y, z);

            if (state == null) {
                if (attempt < MAX_RETRIES) {
                    logger.atInfo().log("[Gravestones] State null at (" + x + ", " + y + ", " + z + "), retry " + (attempt + 1) + "/" + MAX_RETRIES);
                    executor.schedule(() -> world.execute(() -> fillContainer(world, x, y, z, items, attempt + 1)), RETRY_DELAY, TimeUnit.MILLISECONDS);
                } else {
                    logger.atWarning().log("[Gravestones] Failed to get block state after " + MAX_RETRIES + " retries at (" + x + ", " + y + ", " + z + ")");
                }
                return;
            }

            if (state instanceof ItemContainerState) {
                ItemContainerState container = (ItemContainerState) state;
                SimpleItemContainer inv = new SimpleItemContainer((short) Math.max(items.size(), 1));
                container.setItemContainer(inv);
                inv.addItemStacks(items);
                logger.atInfo().log("[Gravestones] Filled container with " + items.size() + " items at (" + x + ", " + y + ", " + z + ")");
            } else {
                logger.atWarning().log("[Gravestones] Block state not ItemContainerState at (" + x + ", " + y + ", " + z + ") - type: " + state.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.atWarning().log("[Gravestones] Container fill error: " + e.getMessage());
        }
    }
}
