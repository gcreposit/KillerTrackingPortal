package com.example.killertrackingportal.serviceImpl;
import com.example.killertrackingportal.entity.Location;
import com.example.killertrackingportal.entity.User;
import com.google.cloud.firestore.*;

import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FirestoreRealtimeService {
    private Firestore firestore = FirestoreClient.getFirestore();
    private final SimpMessagingTemplate messagingTemplate;

    public FirestoreRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }


    // This method will be called after the bean is created
    @PostConstruct
    public void listenToUserLocations() {
        CollectionReference usersCollection = firestore.collection("users");

        usersCollection.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                System.err.println("Listen failed: " + e);
                return;
            }
            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                String userId = dc.getDocument().getId();
                switch (dc.getType()) {
                    case ADDED:
                    case MODIFIED:
                        // Listen to the user's locationData subcollection
                        listenToLocationData(userId);
                        break;
                    case REMOVED:
                        // Handle user removal if needed
                        break;
                }
            }
        });
    }

//    This is for all to fetc location history of all users
//    private void listenToLocationData(String userId) {
//        CollectionReference locationData = firestore.collection("users").document(userId).collection("locationData");
//        locationData.addSnapshotListener((locSnapshots, locE) -> {
//            if (locE != null) {
//                log.error("Location listen failed: {}", locE.getMessage());
//                return;
//            }
//            List<Location> locations = new ArrayList<>();
//            for (DocumentSnapshot locDoc : locSnapshots.getDocuments()) {
//                Double latitude = locDoc.getDouble("latitude");
//                Double longitude = locDoc.getDouble("longitude");
//                String timestamp = "";
//                com.google.cloud.Timestamp firestoreTimestamp = locDoc.getTimestamp("timestamp");
//                if (firestoreTimestamp != null) {
//                    timestamp = firestoreTimestamp.toDate().toString();
//                }
//                if (latitude != null && longitude != null) {
//                    locations.add(new Location(latitude, longitude, timestamp));
//                }
//            }
//            // Build a user update object (you can use your User class or a DTO)
//            User user = getUserById(userId); // Implement this to fetch user details
//            user.setLocationHistory(locations);
//
//            // Send update to all subscribers
//            messagingTemplate.convertAndSend("/topic/location", user);
//
//            log.info("Sent location update for user {} with {} locations", userId, locations.size());
//        });
//    }

    // Modify this part to filter data based on the last 2 hours
    private void listenToLocationData(String userId) {
        CollectionReference locationData = firestore.collection("users").document(userId).collection("locationData");
        locationData.addSnapshotListener((locSnapshots, locE) -> {
            if (locE != null) {
                log.error("Location listen failed: {}", locE.getMessage());
                return;
            }

            List<Location> locations = new ArrayList<>();
            Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS); // Get the timestamp for 2 hours ago

            // Fetch the user's tracking status

            for (DocumentSnapshot locDoc : locSnapshots.getDocuments()) {
                Double latitude = locDoc.getDouble("latitude");
                Double longitude = locDoc.getDouble("longitude");
                com.google.cloud.Timestamp firestoreTimestamp = locDoc.getTimestamp("timestamp");

                // Check if timestamp is available
                if (firestoreTimestamp != null) {
                    Instant timestamp = firestoreTimestamp.toDate().toInstant();
                    if (timestamp.isAfter(twoHoursAgo)) { // Only add locations within the last 2 hours
                        locations.add(new Location(latitude, longitude, timestamp.toString()));
                    }
                }
            }

            if (!locations.isEmpty()) {
                // Build a user update object (you can use your User class or a DTO)
                User user = getUserById(userId);
                user.setLocationHistory(locations);

                String busNo = user.getBusNo();

                user.setBusNo(busNo);
                log.info("User {} is on bus {}", userId, busNo);

                // Send update to all subscribers
                messagingTemplate.convertAndSend("/topic/location", user);

                log.info("Sent location update for user {} with {} recent locations", userId, locations.size());
            }
        });
    }
    private User getUserById(String userId) {
        try {
            DocumentReference userDocRef = firestore.collection("users").document(userId);
            DocumentSnapshot userSnapshot = userDocRef.get().get();
            if (userSnapshot.exists()) {
                User user = userSnapshot.toObject(User.class);
                log.info("User snapshot is {}", userSnapshot);
                // Optionally, set the docId field if needed
                if (user != null) {
                    user.setDocId(userId);
                    String busNo = userSnapshot.getString("bus_no");
                    log.info("User {} is on bus {}", userId, busNo);
                    if (busNo != null) {
                        user.setBusNo(busNo); // Set the busNo field
                    }

                }
                return user;
            }
        } catch (Exception e) {
            log.error("Error fetching user by id {}: {}", userId, e.getMessage());
        }
        return null;
    }
}
