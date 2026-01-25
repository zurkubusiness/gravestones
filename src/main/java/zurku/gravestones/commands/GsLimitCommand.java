package zurku.gravestones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import zurku.gravestones.GravestoneSettings;
import javax.annotation.Nonnull;

public class GsLimitCommand extends CommandBase {

    private final GravestoneSettings settings;

    public GsLimitCommand(GravestoneSettings settings) {
        super("gslimit", "Set max gravestones per player (0=unlimited)");
        setAllowsExtraArguments(true);
        setPermissionGroups(new String[]{"OP"});
        this.settings = settings;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String input = ctx.getInputString().trim();
        String[] parts = input.split("\\s+");
        
        if (parts.length < 2) {
            int current = settings.getMaxPerPlayer();
            ctx.sendMessage(Message.raw("Current limit: " + (current == 0 ? "unlimited" : current + " per player")));
            ctx.sendMessage(Message.raw("Usage: /gslimit <count> (0 for unlimited)"));
            return;
        }

        try {
            int limit = Integer.parseInt(parts[1]);
            int clamped = Math.max(0, limit);
            settings.setMaxPerPlayer(clamped);
            ctx.sendMessage(Message.raw("Max gravestones per player: " + (clamped == 0 ? "unlimited" : clamped)));
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("Invalid number. Usage: /gslimit <count>"));
        }
    }
}
