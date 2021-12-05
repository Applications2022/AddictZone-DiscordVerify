package de.ruben.discordverify.command;

import de.ruben.discordverify.DiscordVerify;
import de.ruben.discordverify.manager.DiscordVerifyPlayerManager;
import de.ruben.discordverify.model.VerfiyState;
import de.ruben.discordverify.model.VerifyPlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class LinkCommand extends Command {
    public LinkCommand() {
        super("dlink");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)){
            sender.sendMessage(new ComponentBuilder("§9§lAddictZone §8➜ §7Du musst ein Spieler sein!").create());
            return;
        }

        ProxiedPlayer proxiedPlayer = (ProxiedPlayer) sender;

        proxiedPlayer.sendMessage(new ComponentBuilder("§9§lAddictZone §8➜ §7Bitte warte...").create());
        DiscordVerify.getCustomExecutor().execute(() -> {
            DiscordVerifyPlayerManager discordVerifyPlayerManager = new DiscordVerifyPlayerManager(proxiedPlayer.getUniqueId());

            if(discordVerifyPlayerManager.hasAlreadyAttemptedVerify()){

                VerifyPlayer verifyPlayer = discordVerifyPlayerManager.getVerifyPlayer();

                if(verifyPlayer.getVerifyState() == VerfiyState.WAIT_CONFIRMATION) {
                    proxiedPlayer.sendMessage(new ComponentBuilder("§9§lAddictZone §8➜ §7Du hast bereits einen Verifizierungscode erhalten! §7(§b" + discordVerifyPlayerManager.getVerifyPlayer().getCode() + "§7)").create());
                }else{
                    proxiedPlayer.sendMessage(new ComponentBuilder("§9§lAddictZone §8➜ §7Du hast deinen Minecraft Account bereits mit einem Discord Account verknüpft. Wenn du deinen Discord Account ändern willst melde dich im Support!").create());
                }
            }else{
                VerifyPlayer verifyPlayer = new VerifyPlayer(discordVerifyPlayerManager.createVerifyPlayer());

                proxiedPlayer.sendMessage(new ComponentBuilder("§9§lAddictZone §8➜ §7Dein Verifizierungscode lautet: §b"+verifyPlayer.getCode()).create());
            }
        });

    }
}
