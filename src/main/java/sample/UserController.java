package sample;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
public class UserController {

    List<User> users = new ArrayList<User>();
    List<String> logs = new ArrayList<>();
    List<String> messages = new ArrayList<>();

    private String getTime() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.now();
        return dateTimeFormatter.format(localDateTime);
    }

    private User getUser(String login) throws JSONException {
        Database database = new Database();
        JSONObject object = database.getUser(login);
        if (object != null) return new User(object.getString("fname"), object.getString("lname"), object.getString("login"), object.getString("password"));
        else return null;
    }

    private boolean existLogin(String login) throws JSONException {
        Database database = new Database();
        return  database.existLogin(login);
    }

    public String hash(String password) { return BCrypt.hashpw(password, BCrypt.gensalt(8)); }

    public boolean checkPassword(String login, String password) throws JSONException {
        User user = getUser(login);
        if (user.getLogin() != null) return BCrypt.checkpw(password, user.getPassword());
        return false;
    }

    public static String generateNewToken() {
        Random rnd = new Random();
        long token = Math.abs(rnd.nextLong());
        String random = Long.toString(token, 16);
        return random;
    }

    public boolean validToken(String token, String user) { return token.equals(user); }

    @RequestMapping(method = RequestMethod.POST, value = "/signup")
    public ResponseEntity<String> signup(@RequestBody String data) throws JSONException {
        JSONObject object = new JSONObject(data);
        if (object.has("fName") && object.has("lName") && object.has("login") && object.has("password")) {
            if (existLogin(object.getString("login"))) {
                JSONObject response = new JSONObject();
                response.put("error", "User already exists");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            String password = object.getString("password");
            if (password.isEmpty()) {
                JSONObject response = new JSONObject();
                response.put("error", "Password field is mandatory");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            String hashPassword = hash(object.getString("password"));
            User user = new User(object.getString("fName"), object.getString("lName"), object.getString("login"), hashPassword);
            users.add(user);
            JSONObject response = new JSONObject();
            response.put("fName", object.getString("fName"));
            response.put("lName", object.getString("lName"));
            response.put("login", object.getString("login"));
            response.put("password", hashPassword);
            response.put("token", "");
            Database database = new Database();
            database.insertUser(response);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        } else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/login")
    public ResponseEntity<String> login(@RequestBody String credential) throws JSONException {
        JSONObject object = new JSONObject(credential);
        String time = getTime();
        if (object.has("login") && object.has("password")) {
            JSONObject response = new JSONObject();
            JSONObject logs = new JSONObject();
            if (object.getString("password").isEmpty() || object.getString("login").isEmpty()) {
                response.put("error", "Password and login are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            String hashPassword = hash(object.getString("password"));
            if (existLogin(object.getString("login")) && checkPassword(object.getString("login"), object.getString("password"))) {
                User user = getUser(object.getString("login"));
                if (user.getFname() == null) return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{}");
                response.put("fName", user.getFname());
                response.put("lName", user.getLname());
                response.put("login", user.getLogin());
                logs.put("type", "login");
                logs.put("login", user.getLogin());
                logs.put("datetime", time);
                this.logs.add(logs.toString());
                String token = generateNewToken();
                response.put("token", token);
                user.setToken(token);
                Database database = new Database();
                database.saveToken(object.getString("login"),token);
                database.saveLog(logs);
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            } else {
                response.put("error", "Incorrect login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
        } else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {
        JSONObject object = new JSONObject(data);
        JSONObject response = new JSONObject();
        String login = object.getString("login");
        User user = getUser(login);
        if (user.getFname() == null) {
            response.put("error", "Login doesn't exist in the database");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        Database database = new Database();
        if (user.getFname() != null && database.existLogin(object.getString("login"))) {
            if (database.existToken(token,user.getLogin())) {
                database.deleteToken(object.getString("login"));
                user.setToken(null);
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
            }
        }
        response.put("error", "Incorrect login or token");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/changePassword")
    public ResponseEntity<String> changePassword(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {
        JSONObject object = new JSONObject(data);
        JSONObject response = new JSONObject();
        User user = getUser(object.getString("login"));
        if (user.getFname() == null) {
            response.put("error", "Incorrect login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        if (object.has("login") && object.has("oldPassword") && object.has("newPassword")) {
            if (user.getLogin().equals(object.getString("login")) && BCrypt.checkpw(object.getString("oldPassword"), user.getPassword())) {
                Database database = new Database();
                if (database.existToken(token, user.getLogin())) {
                    String hashPassword = hash(object.getString("newPassword"));
                    database.changePassword(object.getString("login"),hashPassword);
                    user.setPassword(object.getString("newPassword"));
                    return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
                }
            } else {
                response.put("error", "Incorrect password or token");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
        } else
            response.put("error", "Invalid body request");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }

    @RequestMapping(value = "/logs")
    public ResponseEntity<String> log(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {
        JSONObject object = new JSONObject(data);
        JSONObject response = new JSONObject();
        User user = getUser(object.getString("login"));
        if (user.getLogin() == null ) {
            response.put("error", "Incorrect login ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        if (object.has("login")) {
            if (existLogin(object.getString("login"))) {
                Database database = new Database();
                if (database.existToken(token, user.getLogin())) {
                    List<String> logs = database.getLogs(object.getString("login"));
                    if (logs != null) return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(logs.toString());
                    else {
                        response.put("error" , "No records");
                        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());
                    }
                } else {
                    response.put("error", "Incorrect token");
                    return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
                }
            } else {
                response.put("error", "Login doesn't exist in the database");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
        } else {
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/message/new")
    public ResponseEntity<String> sendMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {
        JSONObject object = new JSONObject(data);
        JSONObject response = new JSONObject();
        User user = getUser(object.getString("from"));
        Database database = new Database();
        if (user.getLname() == null || !database.existToken(token,user.getLogin())) {
            response.put("error", "Incorrect login or token");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        if (object.has("from") && object.has("message") && object.has("to")) {
            if (existLogin(object.getString("from")) && existLogin(object.getString("to"))) {
                String time = getTime();
                response.put("from", object.getString("from"));
                response.put("message", object.getString("message"));
                response.put("to", object.getString("to"));
                response.put("datetime", time);
                JSONArray array = new JSONArray();
                array.put(response);
                messages.add(response.toString());
                database.insertMessage(response);
                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            } else {
                response.put("error", "Incorrect login");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
        } else {
            response.put("mistake", "All fields are mandatory");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/messages")
    public ResponseEntity<String> getMessages(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {
        JSONObject object = new JSONObject(data);
        JSONObject response = new JSONObject();
        User user = getUser(object.getString("login"));
        Database database = new Database();
        if (user.getLogin() == null || !database.existToken(token,user.getLogin())) {
            response.put("error", "Incorrect login or token");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        if (object.has("login")) {
            if (existLogin(object.getString("login"))) {
                response.put("from", object.getString("login"));
                List<String> messagesList = database.getMessages();
                database.closeDatabase();
                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messagesList.toString());
            } else {
                response.put("error", "Incorrect login");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
        } else {
            response.put("error", "Login field is mandatory");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }
}
