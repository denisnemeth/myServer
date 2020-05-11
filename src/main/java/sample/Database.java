package sample;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.mindrot.jbcrypt.BCrypt;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Database {

    public MongoClient getConnection() {
        return new MongoClient("localhost", 27017);
    }

    public boolean addUser(String fname, String lname, String login, String password) {
        MongoClient mongoClient = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("ChatApp");
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("Users");
        if (findLogin(login)) {
            Document users = new Document("fname", fname).append("lname", lname).append("login", login).append("password", hashPassword(password));
            mongoCollection.insertOne(users);
            mongoClient.close();
            return true;
        } else return false;
    }

    public boolean loginUser(String login, String password) {
        MongoClient mongoClient = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("ChatApp");
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("Users");
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.put("login", login);
        Bson bson = Filters.eq("login", login);
        Document document = mongoCollection.find(bson).first();
        User user = getUser(login);
        if (document != null) {
            String hashedPswd = document.getString("password");
            if (!findLogin(login) && BCrypt.checkpw(password, hashedPswd)) {
                if (BCrypt.checkpw(password, user.getPassword())) {
                    BasicDBObject token = new BasicDBObject().append("token", generateToken());
                    user.setToken(token.getString("token"));
                    mongoCollection.updateOne(basicDBObject, new BasicDBObject("$set", token));
                } else {
                    mongoClient.close();
                    return false;
                }
                mongoClient.close();
                return true;
            }
        }
        mongoClient.close();
        return false;
    }

    public boolean logoutUser(String login, String token) {
        MongoClient mongoClient = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("ChatApp");
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("Users");
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.put("login", login);
        basicDBObject.put("token", token);
        BasicDBObject basicDBObject1 = new BasicDBObject();
        basicDBObject1.append("login", login);
        basicDBObject1.append("token", token);
        FindIterable<Document> findIterable = mongoCollection.find(basicDBObject1);
        User user = getUser(login);
        if (!findLogin(login) && checkToken(token))
            if (user.getLogin().equals(login) && findIterable.iterator().hasNext()) {
                mongoCollection.updateOne(basicDBObject, new BasicDBObject("$unset", new BasicDBObject("token", token)));
                user.setToken(token);
                mongoClient.close();
            } else {
                mongoClient.close();
                return false;
            }
        mongoClient.close();
        return true;
    }

    public boolean log(String login, String type) {
        MongoClient mongoClient = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("ChatApp");
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("logs");
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.append("login", login);
        User user = getUser(login);
        if (!findLogin(login) && user.getLogin().equals(login)) {
            mongoCollection.insertOne(new Document().append("type", type).append("login", login).append("datetime", getTime()));
            return true;
        }
        return false;
    }

    public boolean findLogin(String login) {
        MongoClient mongoClient = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("ChatApp");
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("Users");
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.put("login", login);
        long count = mongoCollection.countDocuments(basicDBObject);
        if (count == 0) {
            mongoClient.close();
            return true;
        }
        mongoClient.close();
        return false;
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public User getUser(String login) {
        MongoClient mongoClient = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("ChatApp");
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("Users");
        Bson bson = Filters.eq("login", login);
        Document document = mongoCollection.find(bson).first();
        if (!findLogin(login)) {
            assert document != null;
            return new User(document.getString("fname"), document.getString("lname"), document.getString("login"), document.getString("password"));
        }
        mongoClient.close();
        return null;
    }

    private String generateToken() {
        return Long.toString(Math.abs(new SecureRandom().nextLong()), 16);
    }

    public boolean checkToken(String token) {
        MongoClient mongoClient = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("ChatApp");
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("Users");
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.append("token", token);
        long count = mongoCollection.countDocuments(basicDBObject);
        if (count > 0) {
            mongoClient.close();
            return true;
        }
        mongoClient.close();
        return false;
    }

    public String getTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date();
        return(dateFormat.format(date));
    }
}
