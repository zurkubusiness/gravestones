package zurku.gravestones;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import zurku.gravestones.commands.GsLimitCommand;
import zurku.gravestones.commands.GsModelCommand;
import zurku.gravestones.commands.GsProtectionCommand;
import zurku.gravestones.commands.GsTimerCommand;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import javax.annotation.Nonnull;

@SuppressWarnings({"unchecked", "rawtypes"})
public class GravestonePlugin extends JavaPlugin {

    private GravestoneManager gravestoneManager;
    private GravestoneSettings settings;

    public GravestonePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().atInfo().log("[Gravestones] Initializing...");
        cleanupOldVersionFiles();
        
        settings = new GravestoneSettings(getLogger());
        settings.load();
        gravestoneManager = new GravestoneManager(this, settings);
        
        CollectGravestoneInteraction.setManager(gravestoneManager);
        BreakGravestoneInteraction.setManager(gravestoneManager);
        
        registerInteraction();
        registerDeathListener();
        disableVanillaItemDrop();
        registerBreakListener();
        registerPlayerJoinListener();
        registerCommands();
        
        getLogger().atInfo().log("[Gravestones] Initialized");
    }

    @Override
    protected void start() {
        getLogger().atInfo().log("[Gravestones] Ready");
    }

    @Override
    protected void shutdown() {
        if (gravestoneManager != null) {
            gravestoneManager.shutdown();
        }
        getLogger().atInfo().log("[Gravestones] Shutdown");
    }

    private void registerInteraction() {
        try {
            AssetRegistry.getAssetStore(Interaction.class)
                .loadAssets(getName(), List.of(CollectGravestoneInteraction.INSTANCE, BreakGravestoneInteraction.INSTANCE));
            getCodecRegistry(Interaction.CODEC)
                .register("CollectGravestone", CollectGravestoneInteraction.class, CollectGravestoneInteraction.CODEC);
            getCodecRegistry(Interaction.CODEC)
                .register("BreakGravestone", BreakGravestoneInteraction.class, BreakGravestoneInteraction.CODEC);
        } catch (Exception e) {
            getLogger().atWarning().log("[Gravestones] Interaction registration failed: " + e.getMessage());
        }
    }

    private void cleanupOldVersionFiles() {
        try {
            String userHome = System.getProperty("user.home");
            String[] possiblePaths = {
                userHome + "/AppData/Roaming/Hytale/UserData",
                userHome + "/.hytale/UserData",
                "UserData"
            };
            
            File userDataDir = null;
            for (String path : possiblePaths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    userDataDir = dir;
                    break;
                }
            }
            
            if (userDataDir == null) return;
            
            File oldModsFolder = new File(userDataDir, "Mods/Zurku_Gravestones");
            if (oldModsFolder.exists() && oldModsFolder.isDirectory()) {
                if (deleteDirectory(oldModsFolder)) {
                    getLogger().atInfo().log("[Gravestones] Removed old asset pack folder");
                }
            }
            
            File cachedAssets = new File(userDataDir, "CachedAssets");
            if (cachedAssets.exists()) {
                cleanupGameplayConfigsRecursive(cachedAssets);
            }
            
            cleanupInDirectory(userDataDir);
        } catch (Exception e) {
            getLogger().atWarning().log("[Gravestones] Cleanup error: " + e.getMessage());
        }
    }
    
    private void cleanupInDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().equals("Zurku_Gravestones") || file.getName().contains("Gravestones")) {
                    File gameplayConfigs = new File(file, "Server/GameplayConfigs");
                    if (gameplayConfigs.exists() && gameplayConfigs.isDirectory()) {
                        if (deleteDirectory(gameplayConfigs)) {
                            getLogger().atInfo().log("[Gravestones] Removed old GameplayConfigs");
                        }
                    }
                }
                if (!file.getName().equals("CachedAssets")) {
                    cleanupInDirectory(file);
                }
            }
        }
    }
    
    private void cleanupGameplayConfigsRecursive(File dir) {
        if (dir == null || !dir.exists()) return;
        
        if (dir.getName().equals("GameplayConfigs")) {
            File gravestoneJson = new File(dir, "Gravestone.json");
            if (gravestoneJson.exists()) {
                if (deleteDirectory(dir)) {
                    getLogger().atInfo().log("[Gravestones] Removed old GameplayConfigs");
                }
            }
            return;
        }
        
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        cleanupGameplayConfigsRecursive(file);
                    }
                }
            }
        }
    }
    
    private boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return false;
        
        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return dir.delete();
        } catch (Exception e) {
            return false;
        }
    }

    private void disableVanillaItemDrop() {
        try {
            ComponentRegistryProxy proxy = getEntityStoreRegistry();
            Field field = proxy.getClass().getDeclaredField("registry");
            field.setAccessible(true);
            ((ComponentRegistry) field.get(proxy)).unregisterSystem(DeathSystems.DropPlayerDeathItems.class);
        } catch (Exception e) {
            getLogger().atWarning().log("[Gravestones] Could not disable vanilla drops: " + e.getMessage());
        }
    }

    private void registerDeathListener() {
        try {
            getEntityStoreRegistry().registerSystem((ISystem) new DeathListenerSystem(this));
        } catch (Exception e) {
            getLogger().atSevere().log("[Gravestones] Death listener failed: " + e.getMessage());
        }
    }

    private void registerBreakListener() {
        try {
            getEventRegistry().registerGlobal(BreakBlockEvent.class, event -> {
                String id = event.getBlockType().getId();
                if (id != null && id.contains("Gravestone")) {
                    gravestoneManager.removeGravestoneAtPosition(
                        event.getTargetBlock().getX(),
                        event.getTargetBlock().getY(),
                        event.getTargetBlock().getZ()
                    );
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void registerPlayerJoinListener() {
        try {
            getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
                var world = event.getWorld();
                if (world != null) {
                    gravestoneManager.registerWorld(world);
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void registerCommands() {
        try {
            getCommandRegistry().registerCommand(new GravestoneCommand(settings));
            getCommandRegistry().registerCommand(new GsModelCommand(settings));
            getCommandRegistry().registerCommand(new GsTimerCommand(settings));
            getCommandRegistry().registerCommand(new GsProtectionCommand(settings));
            getCommandRegistry().registerCommand(new GsLimitCommand(settings));
        } catch (Exception e) {
            getLogger().atWarning().log("[Gravestones] Command registration failed: " + e.getMessage());
        }
    }

    public GravestoneManager getGravestoneManager() {
        return gravestoneManager;
    }
}
