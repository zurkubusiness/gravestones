package zurku.gravestones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import zurku.gravestones.GravestoneSettings;
import javax.annotation.Nonnull;

public class GsTimerCommand extends CommandBase {

    private final GravestoneSettings settings;

    public GsTimerCommand(GravestoneSettings settings) {
        super("gstimer", "Set gravestone despawn timer (0-1440 minutes, 0=disabled)");
        setAllowsExtraArguments(true);
        setPermissionGroups(new String[]{"OP"});
        this.settings = settings;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String input = ctx.getInputString().trim();
        String[] parts = input.split("\\s+");
        
        if (parts.length < 2) {
            int current = settings.getDespawnMinutes();
            ctx.sendMessage(Message.raw("Current despawn timer: " + (current == 0 ? "disabled" : current + " minutes")));
            ctx.sendMessage(Message.raw("Usage: /gstimer <minutes> (0-1440, 0 to disable)"));
            return;
        }

        try {
            int minutes = Integer.parseInt(parts[1]);
            int clamped = Math.max(0, Math.min(1440, minutes));
            settings.setDespawnMinutes(clamped);
            ctx.sendMessage(Message.raw("Despawn timer set to: " + (clamped == 0 ? "disabled" : clamped + " minutes")));
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("Invalid number. Usage: /gstimer <minutes>"));
        }
    }
}
