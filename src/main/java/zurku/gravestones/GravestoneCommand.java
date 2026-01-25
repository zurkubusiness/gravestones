package zurku.gravestones;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import javax.annotation.Nonnull;

public class GravestoneCommand extends CommandBase {

    private final GravestoneSettings settings;

    public GravestoneCommand(GravestoneSettings settings) {
        super("gravestone", "Show gravestone commands and current settings");
        setPermissionGroups(new String[]{"OP"});
        this.settings = settings;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("=== Gravestone Settings ==="));
        
        String model = settings.isUseVanillaModel() ? "Vanilla" : "Custom";
        ctx.sendMessage(Message.raw("/gsmodel - Toggle model (Current: " + model + ")"));
        
        int timer = settings.getDespawnMinutes();
        String timerStr = timer == 0 ? "Disabled" : timer + " minutes";
        ctx.sendMessage(Message.raw("/gstimer <minutes> - Set despawn timer (Current: " + timerStr + ")"));
        
        int limit = settings.getMaxPerPlayer();
        String limitStr = limit == 0 ? "Unlimited" : String.valueOf(limit);
        ctx.sendMessage(Message.raw("/gslimit <count> - Set max per player (Current: " + limitStr + ")"));
        
        String protection = settings.isOwnerProtection() ? "Enabled" : "Disabled";
        ctx.sendMessage(Message.raw("/gsprotection - Toggle owner protection (Current: " + protection + ")"));
    }
}
