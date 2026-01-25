package zurku.gravestones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import zurku.gravestones.GravestoneSettings;
import javax.annotation.Nonnull;

public class GsModelCommand extends CommandBase {

    private final GravestoneSettings settings;

    public GsModelCommand(GravestoneSettings settings) {
        super("gsmodel", "Toggle gravestone model (vanilla/custom)");
        setPermissionGroups(new String[]{"OP"});
        this.settings = settings;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        settings.toggleVanillaTombstone();
        String model = settings.isUseVanillaModel() ? "Vanilla" : "Custom";
        ctx.sendMessage(Message.raw("Gravestone model set to: " + model));
    }
}
