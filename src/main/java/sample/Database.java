package sample;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Database {

    /*private JSONObject configObject = new JSONObject();
    private String url = configObject.get("url").toString();
    private int port = Integer.parseInt(configObject.get("port").toString());
    private String database = configObject.get("database").toString();
    private MongoClient mongoClient = new MongoClient(url, port);
    private MongoDatabase mongoDatabase = mongoClient.getDatabase(database);*/
    private MongoClient mongoClient = new MongoClient("localhost", 27017);
    private MongoDatabase mongoDatabase = mongoClient.getDatabase("ChatApp");
    private MongoCollection<Document> collectionUsers = mongoDatabase.getCollection("Users");
    private MongoCollection<Document> collectionLogs = mongoDatabase.getCollection("Logs");
    private MongoCollection<Document> collectionMessages = mongoDatabase.getCollection("Messages");

    public void closeDatabase() {
        this.mongoClient = null;
        this.mongoDatabase = null;
    }

    public void insertUser(JSONObject jsonObject) throws JSONException {
        Document document = new Document()
                .append("fName", jsonObject.getString("fName"))
                .append("lName", jsonObject.getString("lName"))
                .append("login", jsonObject.getString("login"))
                .append("password", jsonObject.getString("password"))
                .append("token", jsonObject.getString("token"));
        collectionUsers.insertOne(document);
    }

    public void insertMessage(JSONObject jsonObject) throws JSONException {
        Document document = new Document()
                .append("from", jsonObject.getString("from"))
                .append("message", jsonObject.getString("message"))
                .append("to", jsonObject.getString("to"))
                .append("datetime", jsonObject.getString("datetime"));
        collectionMessages.insertOne(document);
    }

    public void changePassword(String login, String hash) {
        Bson query = new Document("login", login);
        Bson value = new Document("password", hash);
        Bson update = new Document("$set", value);
        collectionUsers.updateOne(query, update);
    }

    public JSONObject getUser(String login) throws JSONException {
        Document document = collectionUsers.find(new Document("login", login)).first();
        JSONObject object = new JSONObject(document);
        if (document == null) return null;
        else return object;
    }

    public List<String> getUsers() throws JSONException {
        List<String> users = new ArrayList<>();
        for (Document document : collectionUsers.find()) {
            JSONObject object = new JSONObject(document.toJson());
            users.add(object.getString("login"));
        }
        return users;
    }

    public List<String> getMessages() throws JSONException {
        List<String> messages = new ArrayList<>();
        for (Document document : collectionMessages.find()) {
            JSONObject object = new JSONObject(document.toJson());
            messages.add(object.toString());
        }
        return messages;
    }

    public List<String> getLogs(String login ) throws JSONException {
        List<String> logs = new ArrayList<>();
        for (Document document : collectionLogs.find()) {
            JSONObject object = new JSONObject(document.toJson());
            if (object.getString("login").equals(login)) logs.add(object.toString());
        }
        return logs;
    }

    public void saveToken(String login, String token) {
        Bson query = new Document("login", login );
        Bson value = new Document("token", token);
        Bson update = new Document("$set", value);
        collectionUsers.updateOne(query, update);
    }

    public void saveLog(JSONObject jsonObject) throws JSONException {
        Document document = new Document()
                .append("type", jsonObject.getString("type"))
                .append("login", jsonObject.getString("login"))
                .append("datetime", jsonObject.getString("datetime"));
        collectionLogs.insertOne(document);
    }

    public boolean existLogin(String login) throws JSONException {
        Document document = collectionUsers.find(new Document("login", login)).first();
        if (document == null) return false;
        else return true;
    }

    public boolean existToken(String token, String login) throws JSONException {
        try (MongoCursor<Document> mongoCursor = collectionUsers.find().iterator()) {
            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();
                JSONObject object = new JSONObject(document.toJson());
                if (object.getString("login").equals(login) && object.getString("token").equals(token)) return true;
            }
        }
        return false;
    }

    public boolean matchToken(String login, String token) {
        try (MongoCursor<Document> mongoCursor = collectionUsers.find().iterator()) {
            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();
                JSONObject object = new JSONObject(document.toJson());
                if (object.getString("login").equals(login) && object.getString("token").equals(token)) return true;
            }
        } catch (JSONException e) { e.printStackTrace(); }
        return false;
    }

    public void deleteToken(String login) {
        Bson query = new Document("login", login);
        Bson value = new Document("token", "");
        Bson update = new Document("$set", value);
        collectionUsers.updateOne(query, update);
    }
}