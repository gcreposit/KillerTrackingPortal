package com.example.killertrackingportal.serviceImpl;

import com.example.killertrackingportal.service.DataService;
import com.example.killertrackingportal.service.FirebaseAccessTokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class DataServiceImpl implements DataService {


    private final FirebaseAccessTokenService firebaseAccessTokenService;

    private HttpClient httpClient;
    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newHttpClient();
    }
    public DataServiceImpl(FirebaseAccessTokenService firebaseAccessTokenService ){
        this.firebaseAccessTokenService = firebaseAccessTokenService;

    }


    //---------------------------STARTED This Method is to sendNotification To all the Subscribers---------------------

    public Map<String, Integer> sendNotificationToAll(String title, String body, String description, List<Map<String, String>> projectStatusList) throws IOException, InterruptedException {

        String topic = "all";  // Your chosen topic

        int successCount = 0;
        int failureCount = 0;

        String accessToken = firebaseAccessTokenService.getAccessToken();

        String statusDetailsJson = new ObjectMapper().writeValueAsString(projectStatusList);

        String escapedStatusDetailsJson = statusDetailsJson.replace("\"", "\\\"");


// Payload for sending to topic instead of token
        String payload = """
                {
                  "message": {
                    "topic": "%s",
                    "notification": {
                      "title": "%s",
                      "body": "%s"
                    },
                    "data": {
                      "description": "%s",
                      "projectStatusList": "%s"
                    },
                    "android": {
                      "priority": "high",
                      "notification": {
                        "channel_id": "high_importance_channel",
                        "sound": "default"
                      }
                    }
                  }
                }
                """.formatted(topic, title, body + " [" + LocalDateTime.now() + "]", description, escapedStatusDetailsJson);


        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("https://fcm.googleapis.com/v1/projects/track-user-a77c7/messages:send"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Failed to send notification to topic: {}", topic);
            log.error("Response: {}", response.body());
            failureCount++;
            successCount = 0;
        } else {
            log.info("Notification sent successfully to topic: {}", topic);
            log.error("Response: {}", response.body());
        }


        Map<String, Integer> result = new HashMap<>();
        result.put("success", successCount);
        result.put("failure", failureCount);
        return result;
    }

    @Autowired
    private Firestore firestore;


    public void saveAndSendNotification(Map<String, Object> payload) throws Exception {
        String type = (String) payload.get("type");
        String message = (String) payload.get("message");
        String title;
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", type);
        doc.put("timestamp", FieldValue.serverTimestamp());
        doc.put("message", message);

        if ("IC".equals(type)) {
            title = "Notification from Information Centre";
            doc.put("title", title);
        } else if ("CC".equals(type)) {
            title = "Notification from Command Centre";
            doc.put("title", title);
            doc.put("latitude", payload.get("latitude"));
            doc.put("longitude", payload.get("longitude"));
        } else {
            throw new IllegalArgumentException("Invalid type");
        }

        // Save to Firestore (creates collection if not exists)
        firestore.collection("allNotifications").add(doc).get();

        // Send push notification
        saveAndSendNotification(title, message, doc);
    }


    private void saveAndSendNotification(String title, String message, Map<String, Object> data) throws Exception {
        String topic = "all";
        String accessToken = firebaseAccessTokenService.getAccessToken();

        // Prepare data for FCM
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("type", data.get("type"));
        notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));
        notificationData.put("title", title);
        notificationData.put("message", message);
        if ("CC".equals(data.get("type"))) {
            notificationData.put("latitude", data.get("latitude"));
            notificationData.put("longitude", data.get("longitude"));
        }

        String payload = new ObjectMapper().writeValueAsString(Map.of(
                "message", Map.of(
                        "topic", topic,
                        "notification", Map.of(
                                "title", title,
                                "body", message
                        ),
                        "data", notificationData,
                        "android", Map.of(
                                "priority", "high",
                                "notification", Map.of(
                                        "channel_id", "high_importance_channel",
                                        "sound", "default"
                                )
                        )
                )
        ));

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("https://fcm.googleapis.com/v1/projects/track-user-a77c7/messages:send"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
    //---------------------------ENDED This Method is to sendNotification To all the Subscribers---------------------

