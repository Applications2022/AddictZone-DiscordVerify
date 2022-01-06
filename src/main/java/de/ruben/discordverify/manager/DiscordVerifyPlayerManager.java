package de.ruben.discordverify.manager;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import de.ruben.discordverify.DiscordVerify;
import de.ruben.discordverify.model.VerfiyState;
import de.ruben.discordverify.model.VerifyPlayer;
import gnu.trove.impl.hash.TDoubleCharHash;
import net.md_5.bungee.api.ProxyServer;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DiscordVerifyPlayerManager {



    private final UUID uuid;

    public DiscordVerifyPlayerManager(UUID uuid){
        this.uuid = uuid;
    }

    public void setAcceptConnectedAccount(String account){
        VerifyPlayer verifyPlayer = getVerifyPlayer();
            verifyPlayer.setConnectedAccount(account);
            verifyPlayer.setVerifyState(VerfiyState.CONFIRMED);
            verifyPlayer.setRank(DiscordVerify.getInstance().getLuckPermsApi().getUserManager().getUser(uuid).getPrimaryGroup());
            DiscordVerify.getInstance().getUserMap().putIfAbsent(verifyPlayer.getName().toLowerCase(), Map.entry(verifyPlayer.getName(), verifyPlayer.getConnectedAccount()));
            DiscordVerify.getInstance().getUserNameByIdMap().put(verifyPlayer.getConnectedAccount(), verifyPlayer.getName());
            updateVerifyPlayer(verifyPlayer);
    }

    public static Boolean existVerifyPlayerByCode(String code){
                return getVerifyPlayer(code) != null;

    }

    public static Boolean existVerifyPlayerById(String accountId){
                return getVerifyPlayerByDiscord(accountId) != null;
    }

    public static void setPlayerMaps(){
        Map<String, Map.Entry<String, String>> map = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();

        getCollection().find().into(new ArrayList<>()).forEach(document -> {
            VerifyPlayer verifyPlayer = new VerifyPlayer(document);
            if(verifyPlayer.getVerifyState() == VerfiyState.CONFIRMED) {
                map.putIfAbsent(verifyPlayer.getName().toLowerCase(), Map.entry(verifyPlayer.getName(), verifyPlayer.getConnectedAccount()));
                map2.put(verifyPlayer.getConnectedAccount(), verifyPlayer.getName());
            }
        });


        DiscordVerify.getInstance().setUserMap(map);
        DiscordVerify.getInstance().setUserNameByIdMap(map2);
    }

    public static Boolean hasConfirmedAccount(UUID uuid){
        Document document = getCollection().find(Filters.eq("_id", uuid)).first();

        if(document == null){
            return false;
        }
        VerifyPlayer verifyPlayer = new VerifyPlayer(document);

        if(verifyPlayer.getVerifyState() == VerfiyState.WAIT_CONFIRMATION){
            return false;
        }else{
            return true;
        }
    }

    public void deleteVerifyPlacerById(String accountId){
            getCollection().deleteOne(Filters.eq("connectedAccount", accountId));
    }

    public void updateName(String name){
        DiscordVerify.getCustomExecutor().execute(() -> {
            if(hasAlreadyAttemptedVerify()){
               VerifyPlayer verifyPlayer = getVerifyPlayer();
                if(!verifyPlayer.getName().equals(name)) {
                   if (verifyPlayer.getVerifyState() == VerfiyState.CONFIRMED) {
                       if (DiscordVerify.getInstance().getUserMap().containsKey(verifyPlayer.getName().toLowerCase())) {
                           DiscordVerify.getInstance().getUserMap().remove(verifyPlayer.getName().toLowerCase());
                       }

                       DiscordVerify.getInstance().getUserMap().put(name.toLowerCase(), Map.entry(name, verifyPlayer.getConnectedAccount()));

                       if (DiscordVerify.getInstance().getUserNameByIdMap().containsKey(verifyPlayer.getConnectedAccount())) {
                           DiscordVerify.getInstance().getUserNameByIdMap().replace(verifyPlayer.getConnectedAccount(), name);
                       } else {
                           DiscordVerify.getInstance().getUserNameByIdMap().put(verifyPlayer.getConnectedAccount(), name);
                       }
                   }

                    DiscordVerify.getInstance().getJda().retrieveUserById(verifyPlayer.getConnectedAccount()).queue(user -> {
                        DiscordVerify.getInstance().getJda().getGuilds().forEach(guild -> {
                            guild.retrieveMember(user).queue(member -> {
                                member.modifyNickname(member.getRoles().get(0).getName() + " | " + name).queue();
                            });
                        });
                    });


                   verifyPlayer.setName(name);
                   updateVerifyPlayer(verifyPlayer);
                }

            }
        });
    }

    public VerifyPlayer getVerifyPlayer(){
                                Document document = getCollection().find(Filters.eq("_id", uuid)).first();

                                if(document == null) {
                                    return new VerifyPlayer(createVerifyPlayer());
                                }else {
                                    return new VerifyPlayer(document);
                                }



    }

    public static VerifyPlayer getVerifyPlayer(String code){

            Document document = getCollection().find(Filters.eq("code", code)).first();

            if(document == null) {
                return null;
            }else {
                return new VerifyPlayer(document);
            }

    }

    public static VerifyPlayer getVerifyPlayerByDiscord(String accountId){

            Document document = getCollection().find(Filters.eq("connectedAccount", accountId)).first();

            if(document == null) {
                return null;
            }else {
                return new VerifyPlayer(document);
            }



    }

    private Document updateVerifyPlayer(Document document){
        getCollection().replaceOne(Filters.eq("_id", uuid), document);
        return document;
    }

    private Document updateVerifyPlayer(VerifyPlayer verifyPlayer){
        getCollection().replaceOne(Filters.eq("_id", uuid), verifyPlayer.toDocument());
        return verifyPlayer.toDocument();
    }

    public Document createVerifyPlayer(){
        VerifyPlayer verifyPlayer = new VerifyPlayer(uuid,
                ProxyServer.getInstance().getPlayer(uuid).getName(),
                "", RandomStringUtils.randomAlphanumeric(9),
                VerfiyState.WAIT_CONFIRMATION,
                DiscordVerify.getInstance().getLuckPermsApi().getUserManager().getUser(uuid).getPrimaryGroup());

        while(getCollection().find(Filters.eq("code", verifyPlayer.getCode())).first() != null){
            verifyPlayer.setCode(RandomStringUtils.randomAlphanumeric(9));
        }

        UpdateResult updateResult = getCollection().replaceOne(Filters.eq("_id", uuid), verifyPlayer.toDocument());

        if(updateResult.getMatchedCount() <= 0){
            getCollection().insertOne(verifyPlayer.toDocument());
        }

        return verifyPlayer.toDocument();
    }

    public boolean hasAlreadyAttemptedVerify(){
        return getCollection().find(Filters.eq("_id", uuid)).first() != null;
    }


    private static MongoCollection<Document> getCollection(){
        return DiscordVerify.getInstance().getMongoDBStorage().getMongoClient().getDatabase("Discord").getCollection("Data_DiscordVerify");
    }
}
