package com.example.killertrackingportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public interface DataService {
    Map<String, Integer> sendNotificationToAll(String title, String body, String description, List<Map<String, String>> projectStatusList) throws IOException, InterruptedException;

    void saveAndSendNotification(Map<String, Object> payload) throws Exception;
}
