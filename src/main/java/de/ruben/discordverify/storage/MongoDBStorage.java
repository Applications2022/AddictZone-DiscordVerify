package de.ruben.discordverify.storage;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MongoDBStorage {


    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private final MongoClientOptions mongoClientOptions;
    private final String host;
    private final String database;
    private final int port;
    private final String user;
    private final String password;

    private final ExecutorService executorService;

    public MongoDBStorage(int poolsize, String host, String database, int port, String user, String password, MongoClientOptions mongoClientOptions) {
        this.host = host;
        this.database = database;
        this.port = port;
        this.user = user;
        this.password = password;
        this.mongoClientOptions = mongoClientOptions;
        //this.codecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),CodecRegistries.fromProviders(new UuidCodecProvider(UuidRepresentation.JAVA_LEGACY)));
        this.executorService = Executors.newFixedThreadPool(poolsize, new DefaultThreadFactory(database+" - "));
    }

    public MongoDBStorage(String host, String database, int port, String user, String password, MongoClientOptions mongoClientOptions) {
        this(2, host, database, port, user, password, mongoClientOptions);
    }

    public MongoDBStorage(String host, String database, int port, MongoClientOptions mongoClientOptions) {
        this(2, host, database, port, "", "", mongoClientOptions);
    }

    public CompletableFuture<Document> insertOneDocument(String collection, Document document){
        MongoCollection<Document> mongoCollection = getCollection(collection);

        return CompletableFuture.supplyAsync(() -> {
            mongoCollection.insertOne(document);
            return document;
        }, executorService);
    }

    public CompletableFuture<List<Document>> insertDocument(String collection, List<Document> document){
        MongoCollection<Document> mongoCollection = getCollection(collection);

        return CompletableFuture.supplyAsync(() -> {
            mongoCollection.insertMany(document);
            return document;
        }, executorService);
    }

    public CompletableFuture<List<Document>> insertDocument(String collection, Document... documents){
        MongoCollection<Document> mongoCollection = getCollection(collection);

        return CompletableFuture.supplyAsync(() -> {
            mongoCollection.insertMany(Arrays.asList(documents));
            return Arrays.asList(documents);
        }, executorService);
    }

    public CompletableFuture<Document> getDocumentByDocument(String collection, Document document){
        return getDocumentByBson(collection, document);
    }

    public CompletableFuture<Document> getDocumentByBson(String collection, Bson bson){
        MongoCollection<Document> mongoCollection = getCollection(collection);
        return CompletableFuture.supplyAsync(() -> mongoCollection.find(bson).first(), executorService);
    }

    public CompletableFuture<Document> updateDocument(String collection, Bson document, Bson updatedDocument){
        MongoCollection<Document> mongoCollection = getCollection(collection);
        return CompletableFuture.supplyAsync(() -> mongoCollection.findOneAndUpdate(document, updatedDocument));
    }

    public CompletableFuture<Document> replaceDocument(String collection, Document document, Document replacement){
        return CompletableFuture.supplyAsync(() -> getCollection(collection).findOneAndUpdate(document, replacement));
    }

    public void createAscendingIndex(String collection, String... fields){
        createIndex(collection, Indexes.ascending(fields));
    }

    public void createDescendingIndex(String collection, String... fields){
        createIndex(collection, Indexes.descending(fields));
    }

    public void createIndex(String collection, Bson bson){
        MongoCollection<Document> mongoCollection = getCollection(collection);
        executorService.execute(() -> mongoCollection.createIndex(bson));
    }


    private final MongoCollection<Document> getCollection(String name) {
        try {
            MongoCollection<Document> collection = getMongoDatabase().getCollection(name);
            return getMongoDatabase().getCollection(name);
        }
        // Collection does not exist
        catch (IllegalArgumentException e) {
            getMongoDatabase().createCollection(name);
            return getCollection(name);
        }

    }

    public void connect() {
        if (user.isEmpty() && password.isEmpty())
            this.mongoClient = new MongoClient(new ServerAddress(host, port), mongoClientOptions);
        else
            this.mongoClient = new MongoClient(new ServerAddress(host, port), List.of(MongoCredential.createCredential(user, database, password.toCharArray())), mongoClientOptions);
        this.mongoDatabase = mongoClient.getDatabase(database);
    }

    public void disconnect() {
        this.mongoClient.close();
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    private CodecRegistry codecRegistry(){
        return CodecRegistries.fromRegistries(
                CodecRegistries.fromProviders(new UuidCodecProvider(UuidRepresentation.STANDARD)),
                MongoClientSettings.getDefaultCodecRegistry()
        );
    }


    public Binary toStandardBinaryUUID(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        byte[] uuidBytes = new byte[16];

        for (int i = 15; i >= 8; i--) {
            uuidBytes[i] = (byte) (lsb & 0xFFL);
            lsb >>= 8;
        }

        for (int i = 7; i >= 0; i--) {
            uuidBytes[i] = (byte) (msb & 0xFFL);
            msb >>= 8;
        }

        return new Binary((byte) 0x03, uuidBytes);
    }
}
