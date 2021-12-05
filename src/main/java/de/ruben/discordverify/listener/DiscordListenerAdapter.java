package de.ruben.discordverify.listener;

import de.ruben.discordverify.DiscordVerify;
import de.ruben.discordverify.manager.DiscordVerifyPlayerManager;
import de.ruben.discordverify.model.VerfiyState;
import de.ruben.discordverify.model.VerifyPlayer;
import lombok.Builder;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class DiscordListenerAdapter extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        DiscordVerify.getCustomExecutor().execute(() -> {

            try {
                if(event.isFromGuild()) {
                    Guild guild = event.getGuild();

                    if (guild != null && guild.getId().equalsIgnoreCase("640950922842865671")) {
                        TextChannel textChannel = event.getTextChannel();

                        if (textChannel.getId().equalsIgnoreCase("892082169571332157")) {
                            String[] args = event.getMessage().getContentRaw().split(" ");

                            if (args[0].equalsIgnoreCase("!confirm")) {
                                System.out.println(5);

                                if (args.length <= 1) {
                                    Message message = textChannel.sendMessage("Bitte benutze: !confirm <code>").complete(false);
                                    event.getMessage().delete().queue(unused -> {
                                        message.delete().queueAfter(5, TimeUnit.SECONDS);
                                    });


                                    return;
                                }

                                String code = args[1];


                                if (!DiscordVerifyPlayerManager.existVerifyPlayerByCode(code)) {
                                    Message message = textChannel.sendMessage("Hast du dich verschrieben? Zu Diesem Code wurde kein Minecraft Benutzer gefunden.").complete(false);
                                    event.getMessage().delete().queue(unused -> {
                                        message.delete().queueAfter(5, TimeUnit.SECONDS);
                                    });
                                    return;
                                } else {
                                    VerifyPlayer verifyPlayer = DiscordVerifyPlayerManager.getVerifyPlayer(code);

                                    if (verifyPlayer.getVerifyState() == VerfiyState.CONFIRMED) {

                                        Message message = textChannel.sendMessage("Der Spieler, zu dem dieser Code gehört hat sich bereits verifiziert!").complete(false);

                                        event.getMessage().delete().queue(unused -> {
                                            message.delete().queueAfter(5, TimeUnit.SECONDS);
                                        });


                                    } else {
                                        if (ProxyServer.getInstance().getPlayer(verifyPlayer.getUuid()) == null || !ProxyServer.getInstance().getPlayer(verifyPlayer.getUuid()).isConnected()) {
                                            Message message = textChannel.sendMessage("Du musst mit deinem Minecraft Account mit dem AddictZone.net Minecraft server verbunden sein, damit du dich verifizieren kannst!").complete();

                                            event.getMessage().delete().queue(unused -> {
                                                message.delete().queueAfter(5, TimeUnit.SECONDS);
                                            });
                                            return;
                                        }

                                        DiscordVerifyPlayerManager discordVerifyPlayerManager = new DiscordVerifyPlayerManager(verifyPlayer.getUuid());

                                        discordVerifyPlayerManager.setAcceptConnectedAccount(event.getMember().getUser().getId());

                                        String rankName = verifyPlayer.getRank();

                                        if (rankName.equalsIgnoreCase("default")) {
                                            rankName = "spieler";
                                        }

                                        guild.addRoleToMember(event.getMember(), guild.getRolesByName(rankName, true).get(0)).queue();
                                        guild.addRoleToMember(event.getMember(), guild.getRolesByName("verifiziert", true).get(0)).queue();
                                        guild.modifyNickname(event.getMember(), guild.getRolesByName(rankName, true).get(0).getName() + " | " + verifyPlayer.getName()).queue();

                                        ProxyServer.getInstance().getPlayer(verifyPlayer.getUuid()).sendMessage(new ComponentBuilder("§9§lAddictZone §8➜ §7Du hast dich erfolgreich mit dem Discord Account §b" + event.getMember().getUser().getAsTag() + " §7verbunden!").create());

                                        Message message = textChannel.sendMessage("Du hast dich erfolgreich mit dem Minecraft Account " + verifyPlayer.getName() + " verbunden!").complete(false);
                                        event.getMessage().delete().queue(unused -> {
                                            message.delete().queueAfter(5, TimeUnit.SECONDS);
                                        });


                                    }

                                }

                            } else {
                                if (!event.getMember().getUser().getId().equalsIgnoreCase("916664174799646790")) {
                                    event.getMessage().delete().queue();
                                }
                            }

                        }

                    }
                }
            } catch (RateLimitedException e) {
                e.printStackTrace();
            }
        });

        super.onMessageReceived(event);
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {

        if(event.getName().equals("msg")) {

            PrivateChannel privateChannel = event.getPrivateChannel();

            String message = event.getOption("message").getAsString();

            String[] args = message.split(" ");


            String playerName = event.getOption("name").getAsString();
            String msg = "";
            for (int i = 0; i != args.length; i++) {
                msg = msg + args[i] + " ";
            }

            ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(playerName);

            String fromName = DiscordVerify.getInstance().getUserNameByIdMap().get(privateChannel.getUser().getId());

            if (proxiedPlayer == null || !proxiedPlayer.isConnected()) {
                if (DiscordVerify.getInstance().getUserMap().containsKey(playerName.toLowerCase())) {

                    String toUserId = DiscordVerify.getInstance().getUserMap().get(playerName.toLowerCase()).getValue();

                    if (!toUserId.equals(event.getUser().getId())) {
                        String finalMsg1 = msg;
                        DiscordVerify.getInstance().getJda().openPrivateChannelById(toUserId).queue(privateChannel1 -> {
                            privateChannel1.sendMessage("[" + fromName + " ➜ Mir] " + ChatColor.stripColor(finalMsg1)).submit(true);
                            event.reply("[Ich ➜ " + DiscordVerify.getInstance().getUserNameByIdMap().get(toUserId) + "] " + ChatColor.stripColor(finalMsg1)).queue();
                        });
                    } else {
                        event.reply("Du kannst dir selbst keine privaten Nachrichten senden!").queue(interactionHook -> {
                            interactionHook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
                        });
                    }

                } else {
                    event.reply("Dieser Spieler ist nicht Online und hat sich nicht mit Discord gelinkt!").queue(interactionHook -> {
                        interactionHook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
                    });
                }
            } else {
                if(!DiscordVerify.getInstance().getUserMap().get(playerName.toLowerCase()).getKey().equals(fromName)) {
                    String finalMsg = msg;
                    proxiedPlayer.sendMessage(
                            new ComponentBuilder("§9§lAddictZone §8➜ §7(§cDiscord§7) §7[§b" + fromName + " §8➜ §bMir§7] §f" + ChatColor.stripColor(finalMsg)
                            ).create());
                    event.reply("[Ich ➜ " + proxiedPlayer.getName() + "] " + ChatColor.stripColor(finalMsg)).queue();
                }else{
                    event.reply("Du kannst dir selbst keine privaten Nachrichten senden!").queue(interactionHook -> {
                        interactionHook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
                    });
                }
            }
        }

    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        User user = member.getUser();
        guild.modifyNickname(member, "Unverifiziert | " + user.getName()).queue();
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        Member member = event.getMember();

        if (DiscordVerifyPlayerManager.existVerifyPlayerById(member.getUser().getId())) {
            VerifyPlayer verifyPlayer = DiscordVerifyPlayerManager.getVerifyPlayerByDiscord(member.getUser().getId());
            new DiscordVerifyPlayerManager(verifyPlayer.getUuid()).deleteVerifyPlacerById(member.getUser().getId());
        }

    }
}
