package com.example.killertrackingportal.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {


    @Value("${firebase.credentials}")
    private String SERVICE_ACCOUNT_FILE;

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    @PostConstruct
    public void init() throws IOException {
        logger.info("Initializing Firebase configuration...");
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream(SERVICE_ACCOUNT_FILE)) {
                if (serviceAccount == null) {
                    logger.error("Firebase service account file not found in resources.");
                    throw new IOException("Firebase service account file not found in resources.");
                }

                logger.info("Firebase service account file found. Building Firebase options...");
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("FirebaseApp successfully initialized.");
            } catch (IOException e) {
                logger.error("Error initializing FirebaseApp: {}", e.getMessage(), e);
                throw e;
            }
        } else {
            logger.info("FirebaseApp already initialized. Skipping initialization.");
        }
    }
}