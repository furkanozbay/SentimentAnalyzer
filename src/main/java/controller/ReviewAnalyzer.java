package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.PhoneConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReviewAnalyzer {

    @GetMapping("/analyze/{id}")
    public ResponseEntity analyzeReview(@PathVariable String id) {
        System.out.println("Incoming url: " + id);
        String url = "https://www.akakce.com/yorum/?p=" + id;

        List<String> negativeComments = new ArrayList<>();

        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Elements commentDivs = doc.getElementsByClass("cm");
        for (Element commentDiv : commentDivs) {
            String comment = commentDiv.ownText();
            JsonObject result = new JsonParser().parse(analyzeComment(comment)).getAsJsonObject();
            if (Integer.parseInt(result.get("score").toString()) < 0) {
                negativeComments.add(comment);
            }
        }

        HashMap<String, Integer> resultMap = new HashMap<>();

        for (String comment : negativeComments) {
            String[] tokens = comment.split(" ,;.");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                for (String constant : new PhoneConstants().getAllConstants()) {

                    if ((i < tokens.length - 1 && token.equals("ana") && tokens[i + 1].equals("kart"))) {
                        addOrUpdateMap(resultMap, "anakart");
                    }
                    if (token.toLowerCase().contains(constant)) {
                        addOrUpdateMap(resultMap, constant);
                    }

                }
            }

        }


        for (String key : resultMap.keySet()) {
            System.out.println("Key: " + key + " Count:" + resultMap.get(key));
        }

        return ResponseEntity.ok(resultMap);
    }

    private String analyzeComment(String comment) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HashMap<String, String> map = new HashMap<>();
        map.put("sentence", comment);

        HttpEntity<HashMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:1923/api/analyze", request, String.class);
        return response.getBody();
    }

    private void addOrUpdateMap(Map<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            int temp = map.get(key);
            map.put(key, ++temp);
        } else {
            map.put(key, 1);
        }
    }
}
