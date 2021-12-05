package de.ruben.discordverify.listener;

import de.ruben.discordverify.DiscordVerify;
import de.ruben.discordverify.manager.DiscordVerifyPlayerManager;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class JoinListener implements Listener {

    @EventHandler
    public void onPostJoin(PostLoginEvent event){
        DiscordVerify.getCustomExecutor().execute(() -> {
            new DiscordVerifyPlayerManager(event.getPlayer().getUniqueId()).updateName(event.getPlayer().getName());
        });
    }
}
