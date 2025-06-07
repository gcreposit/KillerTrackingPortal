package com.example.killertrackingportal.controller;


import com.example.killertrackingportal.entity.User;
import com.example.killertrackingportal.service.DataService;
import com.example.killertrackingportal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping(path = "/data")
public class DataController {

    private final UserService userService;
    private final DataService dataService;

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

//    @PostMapping("/sentNotification")
//    public String sendNotification(
//            @RequestParam(value = "type") String type,
//            @RequestParam(value = "message") String message,
//            @RequestParam(value = "latitude", required = false) Double latitude,
//            @RequestParam(value = "longitude", required = false) Double longitude
//            ) throws Exception {
//        String timestamp = String.valueOf(System.currentTimeMillis()); // Current timestamp
//
//        // Create a new notification object
//        Map<String, Object> notification = new HashMap<>();
//        notification.put("type", type);
//        notification.put("message", message);
//        notification.put("timestamp", timestamp);
//        notification.put("latitude", latitude);  // Can be null
//        notification.put("longitude", longitude); // Can be null
//
//
//        // Call the sendNotificationToAll method from the service
////        Map<String, Integer> result = dataService.sendNotificationToAll(title, body, description, projectStatusList);
//
//        // Return a success/failure message based on result
////        if (result.get("failure") == 0) {
////            return "Notification sent successfully!";
////        } else {
////            return "Failed to send notification.";
////        }
//        return null;


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
}
