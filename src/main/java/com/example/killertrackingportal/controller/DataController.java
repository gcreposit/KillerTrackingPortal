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
}
