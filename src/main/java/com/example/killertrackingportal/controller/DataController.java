package com.example.killertrackingportal.controller;


import com.example.killertrackingportal.entity.Uppneda;
import com.example.killertrackingportal.entity.User;
import com.example.killertrackingportal.service.DataService;
import com.example.killertrackingportal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping(path = "/data")
public class DataController {

    private final UserService userService;
    private final DataService dataService;
    @Value("${img.absolute}")
    private String imgDirectory;

    public DataController(
            UserService userService, DataService dataService
    ) {
        this.userService = userService;
        this.dataService = dataService;
    }

    @GetMapping("/get-users")
    public List<User> getUsers(@RequestParam(required = false) Integer hours) throws Exception {
        return userService.getAllUsers(hours);
    }


    @PostMapping("/sendNotification")
    public ResponseEntity<String> sendNotification(@RequestBody Map<String, Object> payload) {
        try {
            dataService.saveAndSendNotification(payload);
            return ResponseEntity.ok("Notification sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to send notification.");
        }

    }


    @GetMapping("/notifications")
    public ResponseEntity<List<Map<String, Object>>> getAllNotifications() {
        List<Map<String, Object>> notifications = dataService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/addMasterData")
    public ResponseEntity<Map<String, Object>> addMasterDataByCsv(@RequestParam MultipartFile file) {
        Map<String, Object> response = dataService.importMasterDataFromCsv(file);
        return ResponseEntity.ok(response);
    }


    @PostMapping(path = "/saveFormData",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> saveFormData(@ModelAttribute Uppneda uppneda) {
        String message=dataService.saveFormData(uppneda);
        return ResponseEntity.ok(message);
    }


    @GetMapping("/fetchAllFormData")
    public  List<Uppneda> fetchAllFormData() {
        List<Uppneda> allData  = dataService.fetchAllFormData();
        // Convert image paths to byte arrays for images
        // Convert image paths to byte arrays for images
        for (Uppneda data : allData) {
            String imagePaths = data.getImgPath();  // Assuming the image paths are stored in the database as a semicolon-separated string

            if (imagePaths != null && !imagePaths.isEmpty()) {
                // Split the imagePaths string by the semicolon
                String[] imagePathArray = imagePaths.split(";");

                List<String> cleanedPaths = new ArrayList<>();

                for (String path : imagePathArray) {
                    String trimmedPath = path.trim();  // Trim leading/trailing spaces
                    if (!trimmedPath.isEmpty()) {
                        cleanedPaths.add(trimmedPath);  // Add only valid paths to the list
                    }
                }
                // Remove the last empty entry if it exists (typically due to the trailing semicolon)
                if (!cleanedPaths.isEmpty() && cleanedPaths.get(cleanedPaths.size() - 1).isEmpty()) {
                    cleanedPaths.remove(cleanedPaths.size() - 1);
                }
                // Create a list to hold the byte arrays of each image
                List<byte[]> imageByteArrays = new ArrayList<>();

                // Convert each image path to a byte array and add to the list
                for (String imagePath : cleanedPaths) {
                    // Trim spaces and ensure no leading slash in the image path
                    imagePath = imagePath.trim();

                    // Remove leading slash from imagePath if it exists
                    if (imagePath.startsWith("/")) {
                        imagePath = imagePath.substring(1);
                    }

                    // Combine the directory path with the image path, ensuring a single slash between them
                    String finalPath = imgDirectory + (imgDirectory.endsWith("/") ? "" : "/") + imagePath; // Ensures no double slashes

                    // Debugging: Print the final path to check the constructed path
                    log.info("Final image path: " + finalPath);

                    // Check if the file exists before attempting to convert it to byte array
                    if (Files.exists(Paths.get(finalPath))) {
                        byte[] imageBytes = convertImageToByteArray(finalPath);
                        imageByteArrays.add(imageBytes);  // Add the image byte array to the list
                    } else {
                        System.err.println("File not found: " + finalPath);
                    }
                }

                // Set the image byte arrays to the Uppneda object (assuming a list of images is required)
                data.setImageData(imageByteArrays);
            }
        }
        return allData;
    }


    // Method to convert image to byte array
    private byte[] convertImageToByteArray(String imagePath) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                System.err.println("File not found: " + imagePath);
                return new byte[0];
            }
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }



}
