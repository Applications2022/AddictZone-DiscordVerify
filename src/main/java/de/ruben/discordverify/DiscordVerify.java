package de.ruben.discordverify;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import de.ruben.discordverify.command.LinkCommand;
import de.ruben.discordverify.listener.DiscordListenerAdapter;
import de.ruben.discordverify.listener.JoinListener;
import de.ruben.discordverify.manager.DiscordVerifyPlayerManager;
import de.ruben.discordverify.storage.MongoDBStorage;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.bson.codecs.configuration.CodecRegistries;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public final class DiscordVerify extends Plugin {

    private static DiscordVerify instance;
    private MongoDBStorage mongoDBStorage;
    private LuckPerms luckPermsApi;
    private JDA jda;

    private static final ScheduledExecutorService customExecutor = Executors.newSingleThreadScheduledExecutor();

    private Map<String, Map.Entry<String, String>> userMap;
    private Map<String, String> userNameByIdMap;

    @Override
    public void onEnable() {
        instance = this;
        //this.mongoDBStorage = new MongoDBStorage( "localhost", "Currency", 27017, MongoClientOptions.builder().codecRegistry(CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(), CodecRegistries.fromCodecs(new TransactionCodec()))).build());
        this.mongoDBStorage = new MongoDBStorage( 10, "localhost", "admin", 27017, "currency", "rni1PbUbxYp4JTrPaMb8", MongoClientOptions.builder().codecRegistry(CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(), CodecRegistries.fromCodecs())).build());

        this.mongoDBStorage.connect();

        this.userMap = new HashMap<>();
        this.userNameByIdMap = new HashMap<>();

        try {
            this.jda = JDABuilder
                    .createDefault("d")
                    .addEventListeners(new DiscordListenerAdapter())
                    .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }

//        for(GatewayIntent gatewayIntent : GatewayIntent.values()){
//            jdaBuilder.enableIntents(gatewayIntent);
//        }
//
//        jdaBuilder.addEventListeners(new DiscordVerifyListener());
//
//        try {
//            this.jda = jdaBuilder.build();
//        } catch (LoginException e) {
//            e.printStackTrace();
//        }

        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                new CommandData("msg", "Schribe eine private Nachricht zu einem Minecraft Spieler.")
                        .addOption(OptionType.STRING, "name", "Minecraft Benutzername des Users dem ihr eine NAchricht schreiben wollt.", true)
                        .addOption(OptionType.STRING, "message", "Die Nachricht an den Spieler.", true)
        );

        commands.queue();

        System.out.println("enabled");

        customExecutor.scheduleAtFixedRate(() -> {
            DiscordVerifyPlayerManager.setPlayerMaps();
        }, 0, 60*3, TimeUnit.SECONDS);



        this.luckPermsApi = LuckPermsProvider.get();

        ProxyServer.getInstance().getPluginManager().registerCommand(this, new LinkCommand());

        ProxyServer.getInstance().getPluginManager().registerListener(this, new JoinListener());
    }

    @Override
    public void onDisable() {
        this.mongoDBStorage.disconnect();

        jda.shutdownNow();
        customExecutor.shutdownNow();
    }

    public static DiscordVerify getInstance() {
        return instance;
    }

    public LuckPerms getLuckPermsApi() {
        return luckPermsApi;
    }


    public static ScheduledExecutorService getCustomExecutor() {
        return customExecutor;
    }

    public void setUserMap(Map<String, Map.Entry<String, String>> userMap) {
        this.userMap = userMap;
    }

    public Map<String, Map.Entry<String, String>> getUserMap() {
        return userMap;
    }

    public void setUserNameByIdMap(Map<String, String> userNameByIdMap) {
        this.userNameByIdMap = userNameByIdMap;
    }
}
