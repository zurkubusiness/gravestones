package zurku.gravestones;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GravestoneSettings {

    private static final String SETTINGS_PATH = "plugins/Gravestones/settings.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_VERSION = 2;
    @SuppressWarnings("unused")
    private final int settingsVersion = CURRENT_VERSION;
    private boolean useVanillaModel = false;
    private int despawnMinutes = 0;
    private int maxPerPlayer = 0;
    private boolean ownerProtection = false;
    
    private transient HytaleLogger logger;

    public GravestoneSettings(HytaleLogger logger) {
        this.logger = logger;
    }

    public boolean isUseVanillaModel() {
        return useVanillaModel;
    }

    public void toggleVanillaTombstone() {
        useVanillaModel = !useVanillaModel;
        save();
    }

    public String getGravestoneBlockId() {
        return useVanillaModel ? "Furniture_Zurku_VanillaGravestone" : "Furniture_Zurku_Gravestone";
    }

    public int getDespawnMinutes() {
        return despawnMinutes;
    }

    public void setDespawnMinutes(int minutes) {
        this.despawnMinutes = minutes;
        save();
    }

    public int getMaxPerPlayer() {
        return maxPerPlayer;
    }

    public void setMaxPerPlayer(int limit) {
        this.maxPerPlayer = limit;
        save();
    }

    public boolean isOwnerProtection() {
        return ownerProtection;
    }

    public void setOwnerProtection(boolean enabled) {
        this.ownerProtection = enabled;
        save();
    }

    public void load() {
        File file = new File(SETTINGS_PATH);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                
                int fileVersion = 1;
                if (json.has("settingsVersion")) {
                    fileVersion = json.get("settingsVersion").getAsInt();
                }
                
                if (json.has("useVanillaModel")) {
                    this.useVanillaModel = json.get("useVanillaModel").getAsBoolean();
                }
                if (json.has("despawnMinutes")) {
                    this.despawnMinutes = json.get("despawnMinutes").getAsInt();
                }
                if (json.has("maxPerPlayer")) {
                    this.maxPerPlayer = json.get("maxPerPlayer").getAsInt();
                }
                if (json.has("ownerProtection")) {
                    this.ownerProtection = json.get("ownerProtection").getAsBoolean();
                }
                
                if (logger != null) {
                    if (fileVersion < CURRENT_VERSION) {
                        logger.atInfo().log("[Gravestones] Settings migrated from v" + fileVersion + " to v" + CURRENT_VERSION);
                    } else {
                        logger.atInfo().log("[Gravestones] Settings loaded (v" + CURRENT_VERSION + ")");
                    }
                }
                
                if (fileVersion < CURRENT_VERSION) {
                    save();
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.atWarning().log("[Gravestones] Failed to load settings, using defaults: " + e.getMessage());
                }
                resetDefaults();
                try {
                    file.delete();
                } catch (Exception ignored) {}
                save();
            }
        } else {
            if (logger != null) {
                logger.atInfo().log("[Gravestones] Creating settings file");
            }
            save();
        }
    }
    
    private void resetDefaults() {
        this.useVanillaModel = false;
        this.despawnMinutes = 0;
        this.maxPerPlayer = 0;
        this.ownerProtection = false;
    }

    public void save() {
        File file = new File(SETTINGS_PATH);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            if (logger != null) {
                logger.atWarning().log("[Gravestones] Failed to save settings: " + e.getMessage());
            }
        }
    }
}
