package de.ruben.discordverify.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.Document;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPlayer {

    private UUID uuid;
    private String name;
    private String connectedAccount;
    private String code;
    private VerfiyState verifyState;
    private String rank;

    public VerifyPlayer(Document document){
        fromDocument(document);
    }

    public Document toDocument(){
        Document document = new Document("_id", uuid);
        document.append("name", name);
        document.append("connectedAccount", connectedAccount);
        document.append("code", code);
        document.append("verifyState", verifyState.toString());
        document.append("rank", rank);

        return document;
    }

    public VerifyPlayer fromDocument(Document document){
        this.uuid = document.get("_id", UUID.class);
        this.name = document.getString("name");
        this.connectedAccount = document.getString("connectedAccount");
        this.code = document.getString("code");
        this.verifyState = VerfiyState.valueOf(document.getString("verifyState"));
        this.rank = document.getString("rank");

        return this;
    }

}
