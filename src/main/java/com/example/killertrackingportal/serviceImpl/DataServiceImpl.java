package com.example.killertrackingportal.serviceImpl;

import com.example.killertrackingportal.entity.Uppneda;
import com.example.killertrackingportal.repository.UppnedaRepo;
import com.example.killertrackingportal.service.DataService;
import com.example.killertrackingportal.service.FirebaseAccessTokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class DataServiceImpl implements DataService {


    private final FirebaseAccessTokenService firebaseAccessTokenService;
    private final UppnedaRepo uppnedaRepo;

    private HttpClient httpClient;

    private final Firestore firestore;

    @Value("${img.directory}")
    private String imgPath;

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public DataServiceImpl(FirebaseAccessTokenService firebaseAccessTokenService, Firestore firestore, UppnedaRepo uppnedaRepo) {
        this.firebaseAccessTokenService = firebaseAccessTokenService;
        this.firestore = firestore;
        this.uppnedaRepo = uppnedaRepo;
    }


    //---------------------------STARTED This Method is to sendNotification To all the Subscribers---------------------


    //------------PART - 1 This method saves the notification to Firestore and sends a push notification to all subscribers--------------------------
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
        }

        else if ("CC".equals(type)) {
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
        saveAndSendPushNotification(title, message, doc);
    }

    @Override
    public List<Map<String, Object>> getAllNotifications() {
        List<Map<String, Object>> notificationList = new ArrayList<>();
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("allNotifications").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot doc : documents) {
                Map<String, Object> data = doc.getData();
                data.put("id", doc.getId());
                notificationList.add(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notificationList;
    }

    @Override
    public String saveFormData(Uppneda uppneda) {

        List<MultipartFile> files = uppneda.getImageFile();
        StringBuilder relativePaths = new StringBuilder();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && !originalFilename.isEmpty()) {
                try {
                    // Generate unique file name (optional: use UUID)
                    String fileName = System.currentTimeMillis() + "_" + originalFilename;
                    String absolutePath = imgPath + File.separator + fileName;
                    File destinationFile = new File(absolutePath);

                    // Create directories if they do not exist
                    if (!destinationFile.getParentFile().exists()) {
                        destinationFile.getParentFile().mkdirs();
                    }

                    // Save the file to the server
                    file.transferTo(destinationFile);

                    // Save relative path in the database (relative to the images folder)
                    relativePaths.append("/images/").append(fileName).append("; ");
                } catch (IOException e) {
                    return "Error while saving image: " + e.getMessage();
                }
            }
        }

        // Store relative image paths in the Uppneda entity and save in DB
        uppneda.setImgPath(relativePaths.toString());

        uppnedaRepo.save(uppneda);

        // Here you can save 'uppneda' object to your database, assuming you have a repository for it.
        // For example: uppnedaRepository.save(uppneda);

        return "Form data and images saved successfully!";
    }

    @Override
    public  List<Uppneda> fetchAllFormData() {
        List<Uppneda> uppnedaList = uppnedaRepo.findAll();
        return uppnedaList;
    }

    //------------PART - 2 This method saves the notification to Firestore and sends a push notification to all subscribers----------------------------------------
    private void saveAndSendPushNotification(String title, String message, Map<String, Object> data) throws Exception {
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

