package sample;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class TimeController {

    @RequestMapping("/time/hour")
    public ResponseEntity<String> getHour() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH");
        Date date = new Date();
        String hour = simpleDateFormat.format(date);
        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{\"hour\":" + hour + "}");
    }
}
