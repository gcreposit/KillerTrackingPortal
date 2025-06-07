package com.example.killertrackingportal.service;

import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;

@Service
public class FirebaseAccessTokenService {

    @Value("${firebase.credentials}")
    private String SERVICE_ACCOUNT_FILE;
    private static final Logger logger = LoggerFactory.getLogger(FirebaseAccessTokenService.class);


    public String getAccessToken() {
        try {
            logger.info("🔐 Loading Firebase credentials from resource: {}", SERVICE_ACCOUNT_FILE);

            InputStream serviceAccount = null;
            String externalFilePath = "/app/resources/" + SERVICE_ACCOUNT_FILE;

            java.io.File externalFile = new java.io.File(externalFilePath);
            if (externalFile.exists()) {
                logger.info("✅ Using external Firebase credentials file at: {}", externalFilePath);
                serviceAccount = new FileInputStream(externalFile);
            } else {
                logger.info("🔍 Falling back to internal classpath resource.");
                serviceAccount = getClass().getClassLoader().getResourceAsStream(SERVICE_ACCOUNT_FILE);
                if (serviceAccount == null) {
                    throw new RuntimeException("❌ Service account file not found in classpath or external location: " + SERVICE_ACCOUNT_FILE);
                }
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount)
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));
            credentials.refreshIfExpired();
            String token = credentials.getAccessToken().getTokenValue();

            logger.info("✅ Firebase access token retrieved successfully.");
            return token;

        } catch (Exception e) {
            logger.error("❌ Failed to load Firebase access token", e);
            throw new RuntimeException("Failed to load Firebase access token", e);
        }
    }

}
