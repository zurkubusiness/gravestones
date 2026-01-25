package zurku.gravestones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import zurku.gravestones.GravestoneSettings;
import javax.annotation.Nonnull;

public class GsProtectionCommand extends CommandBase {

    private final GravestoneSettings settings;

    public GsProtectionCommand(GravestoneSettings settings) {
        super("gsprotection", "Toggle owner-only gravestone access");
        setPermissionGroups(new String[]{"OP"});
        this.settings = settings;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        settings.setOwnerProtection(!settings.isOwnerProtection());
        String status = settings.isOwnerProtection() ? "Enabled" : "Disabled";
        ctx.sendMessage(Message.raw("Owner protection: " + status));
    }
}
