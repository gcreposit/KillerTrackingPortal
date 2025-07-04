package com.example.killertrackingportal.service;

import com.example.killertrackingportal.entity.Uppneda;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public interface DataService {

    void saveAndSendNotification(Map<String, Object> payload) throws Exception;

    List<Map<String, Object>> getAllNotifications();

    Map<String, Object> importMasterDataFromCsv(MultipartFile file);

    String saveFormData(Uppneda uppneda);

    List<Uppneda> fetchAllFormData();
}
