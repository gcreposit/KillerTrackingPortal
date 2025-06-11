package com.example.killertrackingportal.serviceImpl;

import com.example.killertrackingportal.entity.Uppneda;
import com.example.killertrackingportal.repository.UppnedaRepo;
import com.example.killertrackingportal.service.DataService;
import com.example.killertrackingportal.service.FirebaseAccessTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
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
    private final Firestore firestore;
    private HttpClient httpClient;
    @Value("${img.directory}")
    private String imgPath;

    public DataServiceImpl(FirebaseAccessTokenService firebaseAccessTokenService, Firestore firestore, UppnedaRepo uppnedaRepo) {
        this.firebaseAccessTokenService = firebaseAccessTokenService;
        this.firestore = firestore;
        this.uppnedaRepo = uppnedaRepo;
    }

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newHttpClient();
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
            log.error(e.getMessage());
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
                    relativePaths.append("/Land_Details/").append(fileName).append("; ");
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
    public List<Uppneda> fetchAllFormData() {
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


    //---------------------------ENDED This Method is to sendNotification To all the Subscribers---------------------


    //---------------------------START OF MASTER DATA IMPORT SERVICE---------------------

//    @Override
//    public Map<String, Object> importMasterDataFromCsv(MultipartFile file) {
//        List<Map<String, Object>> importedData = new ArrayList<>();
//        List<String> failedRecords = new ArrayList<>();
//        int successCount = 0;
//        int failureCount = 0;
//
//        try (Reader reader = new InputStreamReader(file.getInputStream())) {
//            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
//            CollectionReference collection = firestore.collection("masterData");
//
//            for (CSVRecord record : records) {
//                Map<String, Object> data = new HashMap<>();
//                for (String header : record.toMap().keySet()) {
//                    data.put(header, record.get(header));
//                }
//                try {
//                    String registrationNo = record.get("Registration No.");
//                    DocumentReference docRef = collection.document(registrationNo);
//                    ApiFuture<WriteResult> future = docRef.set(data);
//                    future.get();
//                    importedData.add(data);
//                    successCount++;
//                } catch (Exception ex) {
//                    failureCount++;
//                    failedRecords.add("Failed record: " + data + " | Reason: " + ex.getMessage());
//                    ex.printStackTrace();
//                }
//            }
//
//        } catch (Exception e) {
//
//            log.error("Error processing CSV file: {}", e.getMessage());
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("successCount", successCount);
//        response.put("failureCount", failureCount);
//        response.put("failedRecords", failedRecords);
//        response.put("importedData", importedData);
//
//        return response;
//    }


    @Override
    public Map<String, Object> importMasterDataFromCsv(MultipartFile file) {
        List<Map<String, Object>> importedData = new ArrayList<>();
        List<String> failedRecords = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        try {
            String filename = file.getOriginalFilename();
            CollectionReference collection = firestore.collection("masterData");

            if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
                // Handle Excel (.xlsx)
                Workbook workbook = new XSSFWorkbook(file.getInputStream());
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(cell.getStringCellValue());
                }
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    Map<String, Object> data = new HashMap<>();
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.getCell(j);
                        String value = "";
                        if (cell != null) {
                            if (cell.getCellType() == CellType.NUMERIC) {
                                value = BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                            } else {
                                value = cell.toString();
                            }
                        }
                        data.put(headers.get(j), value);
                    }
                    try {
                        String registrationNo = data.get("Registration No.").toString();
                        DocumentReference docRef = collection.document(registrationNo);
                        ApiFuture<WriteResult> future = docRef.set(data);
                        future.get();
                        importedData.add(data);
                        successCount++;
                    } catch (Exception ex) {
                        failureCount++;
                        failedRecords.add("Failed record: " + data + " | Reason: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
                workbook.close();
            } else {
                // Handle CSV
                try (Reader reader = new InputStreamReader(file.getInputStream())) {
                    Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
                    for (CSVRecord record : records) {
                        Map<String, Object> data = new HashMap<>();
                        for (String header : record.toMap().keySet()) {
                            data.put(header, record.get(header));
                        }
                        try {
                            String registrationNo = record.get("Registration No.");
                            DocumentReference docRef = collection.document(registrationNo);
                            ApiFuture<WriteResult> future = docRef.set(data);
                            future.get();
                            importedData.add(data);
                            successCount++;
                        } catch (Exception ex) {
                            failureCount++;
                            failedRecords.add("Failed record: " + data + " | Reason: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing file: {}", e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("successCount", successCount);
        response.put("failureCount", failureCount);
        response.put("failedRecords", failedRecords);
        response.put("importedData", importedData);

        return response;
    }

}

