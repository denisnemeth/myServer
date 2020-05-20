package sample;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class UserController {

    List<User> users =  new ArrayList<User>();
    List<String> logs =  new ArrayList<String>();
    List<String> messages =  new ArrayList<String>();
    HashMap<String, String> tokens = new HashMap<>();

    public UserController() {}

    public boolean isTokenValid(String token) {
        if (tokens.containsValue(token)) return true;
        return false;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/login")
    public ResponseEntity<String> login(@RequestBody String credential) {
        JSONObject object = new JSONObject(credential);
        if (object.has("login") && object.has("password")) {
            Database database = new Database();
            JSONObject response = new JSONObject();
            if (object.getString("password").isEmpty() || object.getString("login").isEmpty()) {
                response.put("error", "Password and login are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            if(database.findLogin(object.getString("login")) && checkPassword(object.getString("login"), object.getString("password"))) {
                User loggedUser = new Database().getUser(object.getString("login"));
                response.put("fname", loggedUser.getFname());
                response.put("lname", loggedUser.getLname());
                response.put("login", loggedUser.getLogin());
                String token = generateToken();
                response.put("token", token);
                tokens.put(loggedUser.getLogin(), token);
                writeLog("login", loggedUser.getLogin());
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            else {
                response.put("error", "Invalid login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
        }
        else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    private boolean checkPassword(String login, String password) {
        String password2 = new Database().getPassword(login);
        if(password2 != null) if(BCrypt.checkpw(password, password2)) return true;
        return false;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/signup")
    public ResponseEntity<String> signup(@RequestBody String data) {
        JSONObject object = new JSONObject(data);
        if (object.has("fname") && object.has("lname") && object.has("login") && object.has("password")) {
            if(new Database().findLogin(object.getString("login"))) {
                JSONObject response = new JSONObject();
                response.put("error", "User already exists");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            String password = object.getString("password");
            if (password.isEmpty()) {
                JSONObject response = new JSONObject();
                response.put("error", "Password is a mandatory field");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            String hashPassword = hash(object.getString("password"));
            User user = new User(object.getString("fname"), object.getString("lname"), object.getString("login"), hashPassword);
            users.add(user);
            JSONObject response = new JSONObject();
            response.put("fname", object.getString("fname"));
            response.put("lname", object.getString("lname"));
            response.put("login", object.getString("login"));
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    public String hash(String password) { return BCrypt.hashpw(password, BCrypt.gensalt(12)); }

    private boolean existLogin(String login) {
        for (User user : users) if (user.getLogin().equalsIgnoreCase(login)) return true;
        return false;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        JSONObject object = new JSONObject(data);
        String login = object.getString("login");
        User user = getUser(login);
        if (user != null && isTokenValid(token)) {
            user.setToken(null);
            writeLog("logout", user.getLogin());
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
        }
        JSONObject response = new JSONObject();
        response.put("error", "Incorrect login or token");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }

    private String generateToken() {
        int size = 25;
        Random random = new Random();
        String generatedString = "";
        for (int i = 0; i < size; i++) {
            int type = random.nextInt(4);
            switch (type) {
                case 0:
                    generatedString += (char) ((random.nextInt(26)) + 65);
                    break;
                case 1:
                    generatedString += (char) ((random.nextInt(10)) + 48);
                    break;
                default:
                    generatedString += (char) ((random.nextInt(26)) + 97);
            }
        }
        return generatedString;
    }

    @RequestMapping("/time")
    public ResponseEntity<String> getTime(@RequestParam(value = "token") String token) {
        if (token == null) return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        if (isTokenValid(token)) {
            JSONObject response = new JSONObject();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            String time = simpleDateFormat.format(date);
            response.put("time", time);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }

    @RequestMapping("/time/hour")
    public ResponseEntity<String> getHour(@RequestParam(value = "token") String token) {
        if (token == null) return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        if (isTokenValid(token)) {
            JSONObject response = new JSONObject();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH");
            Date date = new Date();
            String time = simpleDateFormat.format(date);
            response.put("hour", time);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }

    private User getUser(String login) {
        for (User user : users) if (user.getLogin().equals(login)) return user;
        return null;
    }

    @RequestMapping("/users")
    public ResponseEntity<String> getUsers(@RequestParam(value = "token") String token) {
        if (token == null) return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        if (isTokenValid(token)) {
            JSONArray array = new JSONArray();
            for (User user : users) {
                JSONObject object = new JSONObject();
                object.put("fname", user.getFname());
                object.put("lname", user.getLname());
                object.put("login", user.getLogin());
                array.put(object);
            }
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(array.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }

    @RequestMapping("/users/{login}")
    public ResponseEntity<String> getOneUser(@RequestParam(value = "token") String token, @PathVariable String login) {
        if (token == null) return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        if (isTokenValid(token)) {
            JSONObject object = new JSONObject();
            User user = getUser(login);
            if (user == null) return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid user name\"}");
            object.put("fname", user.getFname());
            object.put("lname", user.getLname());
            object.put("login", user.getLogin());
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(object.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\")");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/changepassword")
    public ResponseEntity<String> changePassword(@RequestBody String data) {
        JSONObject object = new JSONObject(data);
        if (object.has("oldpassword") && object.has("newpassword") && object.has("login")) {
            String login = object.getString("login");
            String oldpassword = object.getString("oldpassword");
            String newpassword = object.getString("newpassword");
            if (oldpassword.isEmpty() || newpassword.isEmpty()) {
                JSONObject response = new JSONObject();
                response.put("error", "Passwords are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            if (!existLogin(login) || !checkPassword(login, oldpassword)) {
                JSONObject response = new JSONObject();
                response.put("error", "Invalid login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            String hashPassword = hash(object.getString("newpassword"));
            User user = getUser(login);
            user.setPassword(hashPassword);
            writeLog("password change", user.getLogin());
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
        }
        else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    private void writeLog(String type, String login) {
        JSONObject object = new JSONObject();
        object.put("type",type);
        object.put("login",login);
        object.put("datetime",getCurrentDateTime());
        logs.add(object.toString());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/log")
    public ResponseEntity<String> getLogInfo(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        JSONObject object = new JSONObject(data);
        if (object.has("login")) {
            String login = object.getString("login");
            User user = getUser(login);
            if (user == null || !user.getToken().equals(token)) {
                JSONObject response = new JSONObject();
                response.put("error", "Invalid login or token");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            JSONArray response = new JSONArray();
            for (String record : logs) {
                JSONObject temp = new JSONObject(record);
                if (temp.has("login") && temp.getString("login").equals(login)) {
                    response.put(temp);
                }
            }
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request, login is missing");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    private String getCurrentDateTime(){
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date();
        return(dateFormat.format(date));
    }
}
