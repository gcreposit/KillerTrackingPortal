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
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class FirestoreRealtimeService {
    private Firestore firestore = FirestoreClient.getFirestore();
    private final SimpMessagingTemplate messagingTemplate;

    public FirestoreRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    String collectionName = "users";  // ✅ Valid field name


    // This method will be called after the bean is created
    @PostConstruct
    public void listenToUserLocations() {
        CollectionReference usersCollection = firestore.collection(collectionName);

        usersCollection.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                log.error("Firestore collection error", e);
                return;
            }
            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                String userId = dc.getDocument().getId();
                switch (dc.getType()) {
                    case ADDED, MODIFIED -> listenToLocationData(userId);
                    case REMOVED -> {

                    }
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
        CollectionReference locationData = firestore.collection(collectionName).document(userId).collection("locationData");
        locationData.addSnapshotListener((locSnapshots, locE) -> {
            if (locE != null) {
                log.error("Location listen failed: {}", locE.getMessage());
                return;
            }

            List<Location> locations = new ArrayList<>();
            Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS); // Get the timestamp for 2 hours ago

            Location lastLocation = null;
            Instant lastLocationInstant = null;

            // Fetch the user's tracking status

            for (DocumentSnapshot locDoc : locSnapshots.getDocuments()) {
                Double latitude = locDoc.getDouble("latitude");
                Double longitude = locDoc.getDouble("longitude");
                com.google.cloud.Timestamp firestoreTimestamp = locDoc.getTimestamp("timestamp");

                // Check if timestamp is available
                if (firestoreTimestamp != null) {
                    Instant timestamp = firestoreTimestamp.toDate().toInstant();
                    if (lastLocationInstant == null || timestamp.isAfter(lastLocationInstant)) {
                        lastLocationInstant = timestamp;
                        lastLocation = new Location(latitude, longitude, timestamp.toString());
                    }
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
                // --- Set lastActiveAt to the latest location timestamp, even if inactive ---
                if (lastLocation != null) {
                    user.setLastActiveAt(lastLocation.getTimestamp());
                } else {
                    user.setLastActiveAt(null);
                }
                log.info("User {} is on bus {}", userId, busNo);

                // Send update to all subscribers
                messagingTemplate.convertAndSend("/topic/location", user);

                log.info("Sent location update for user {} with {} recent locations", userId, locations.size());
            }
        });
    }
    private User getUserById(String userId) {
        try {
            DocumentReference userDocRef = firestore.collection(collectionName).document(userId);
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
        } catch (InterruptedException ie) {
            // Re-interrupt the thread to preserve the interrupt status
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted while fetching user by id {}", userId, ie);
        } catch (ExecutionException e) {
            log.error("Execution error fetching user by id {}: {}", userId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching user by id {}: {}", userId, e.getMessage(), e);
        }
        return null;
    }
}
