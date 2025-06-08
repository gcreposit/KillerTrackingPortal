package com.example.killertrackingportal.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public interface DataService {
    Map<String, Integer> sendNotificationToAll(String title, String body, String description, List<Map<String, String>> projectStatusList) throws IOException, InterruptedException;

    void saveAndSendNotification(Map<String, Object> payload) throws Exception;

    List<Map<String, Object>> getAllNotifications();

    Map<String, Object> importMasterDataFromCsv(MultipartFile file);
}
