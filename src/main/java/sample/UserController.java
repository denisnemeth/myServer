package sample;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@RestController
public class UserController {
    List<User> list =  new ArrayList<User>();

    public UserController() { list.add(new User("Roman","Simko","roman","heslo")); }

    public boolean isTokenValid(String token) {
        for (User user : list) if (user.getToken() != null && user.getToken().equals(token)) return true;
        return false;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/login")
    public ResponseEntity<String> login(@RequestBody String credential) {
        JSONObject object = new JSONObject(credential);
        if (object.has("login") && object.has("password")) {
            JSONObject response = new JSONObject();
            if (object.getString("password").isEmpty() || object.getString("login").isEmpty()) {
                response.put("error", "Password and login are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            if (existLogin(object.getString("login")) && checkPassword(object.getString("login"), object.getString("password"))) {
                User loggedUser = getUser(object.getString("login"));
                if (loggedUser == null) return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{}");
                response.put("fname", loggedUser.getFname());
                response.put("lname", loggedUser.getLname());
                response.put("login", loggedUser.getLogin());
                String token = generateToken();
                response.put("token", token);
                loggedUser.setToken(token);
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
        User user = getUser(login);
        if (user != null && BCrypt.checkpw(password, user.getPassword())) return true;
        return false;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/signup")
    public ResponseEntity<String> signup(@RequestBody String data){
        JSONObject object = new JSONObject(data);
        if (object.has("fname") && object.has("lname") && object.has("login") && object.has("password")) {
            if (existLogin(object.getString("login"))) {
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
            list.add(user);
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
        for (User user : list) if (user.getLogin().equalsIgnoreCase(login)) return true;
        return false;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        JSONObject object = new JSONObject(data);
        String login = object.getString("login");
        User user = getUser(login);
        if (user != null && isTokenValid(token)) {
            user.setToken(null);
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

    private User getUser(String login) {
        for (User user : list) if (user.getLogin().equals(login)) return user;
        return null;
    }

    @RequestMapping("/users")
    public ResponseEntity<String> getUsers(@RequestParam(value = "token") String token) {
        if (token == null) return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        if (isTokenValid(token)) {
            JSONArray array = new JSONArray();
            for (User user : list) {
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
}
