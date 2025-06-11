package com.example.killertrackingportal.serviceImpl;

import com.example.killertrackingportal.entity.Location;
import com.example.killertrackingportal.entity.User;
import com.example.killertrackingportal.service.UserService;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private Firestore firestore = FirestoreClient.getFirestore();



    // Fetches all users and their location history (oldest to latest)
    public List<User> getAllUsers(Integer hour) {
        CollectionReference usersCollection = firestore.collection("users");
        QuerySnapshot usersSnapshot;

        try {
            usersSnapshot = usersCollection.get().get();
            log.info("Successfully fetched {} users.", usersSnapshot.size());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();  // Restore the interrupted status
            log.warn("Thread was interrupted while fetching users", ie);
            return Collections.emptyList();      // Return empty list gracefully
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage(), e);
            return Collections.emptyList();      // Return empty list in case of an error
        }
        // Calculate cutoff time if hour is provided
        Long cutoffMillis;
        if (hour != null && hour > 0) {
            cutoffMillis = System.currentTimeMillis() - (hour * 60 * 60 * 1000L);
        } else {
            cutoffMillis = null;
        }

        // Map each document into a User object and fetch all location data
        List<User> users = usersSnapshot.getDocuments().stream()
                .map(doc -> {
                    User user = doc.toObject(User.class);
                    log.info("Mapping user: {}", user.getName());
                    // Retrieve the bus_no from Firestore
                    String busNo = doc.getString("bus_no"); // Fetch the bus_no from Firestore
                    user.setBusNo(busNo); // Set bus_no in the user object

                    String registrationNo = doc.getString("registration_no");
                    user.setRegistration_no(registrationNo); // Set registration_no in the user object



                    // Fetch the user's location history (all locations)
                    List<Location> locationHistory = getLocationHistory(doc.getId());
                    List<Location> recentLocations = locationHistory;

                    // If hour filter is set, filter the location history
                    if (cutoffMillis != null) {
                        recentLocations = locationHistory.stream()
                                .filter(loc -> {
                                    Date locDate = parseDate(loc.getTimestamp());
                                    return locDate != null && locDate.getTime() >= cutoffMillis;
                                })
                                .collect(Collectors.toList());
                    }

                    // Set the location history in the user object
                    user.setLocationHistory(recentLocations);

                    // If user is INACTIVE and recentLocations is empty, set last known location
                    if (!user.isTracking() && recentLocations.isEmpty() && !locationHistory.isEmpty()) {
                        List<Location> lastLocList = new ArrayList<>();
                        lastLocList.add(locationHistory.get(locationHistory.size() - 1));
                        user.setLocationHistory(lastLocList);
                        user.setShowLastKnownLocation(true); // Add this flag in User entity
                    }
                    // Set last active at (optional, for your table)
                    if (!locationHistory.isEmpty()) {
                        user.setLastActiveAt(locationHistory.get(locationHistory.size() - 1).getTimestamp());
                    }
                    return user;
                })
                .collect(Collectors.toList());

        log.info("Mapped {} users successfully.", users.size());
        return users;
    }

    // Utility to parse your timestamp string to Date
    private Date parseDate(String timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            return sdf.parse(timestamp);
        } catch (ParseException e) {
            log.error("Error parsing timestamp: {}", timestamp);
            return null;
        }
    }

    // Fetch all location data from the locationData subcollection for a given user
    public List<Location> getLocationHistory(String userId) {
        CollectionReference locationData = firestore.collection("users").document(userId).collection("locationData");

        try {
            // Query for all location data, sorted by timestamp (oldest to latest)
            QuerySnapshot snapshot = locationData.orderBy("timestamp").get().get();

            List<Location> locations = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Double latitude = doc.getDouble("latitude");
                Double longitude = doc.getDouble("longitude");
                // Fetch timestamp from Firestore and convert it to a readable format or milliseconds
                com.google.cloud.Timestamp firestoreTimestamp = doc.getTimestamp("timestamp");
                String timestamp = "";
                if (firestoreTimestamp != null) {
                    timestamp = firestoreTimestamp.toDate().toString();  // Converts to a readable date string
                }
                if (latitude != null && longitude != null) {
                    locations.add(new Location(latitude, longitude,timestamp)); // Add location to the list
                }
            }

            return locations; // Return the full list of locations
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();  // Preserve interrupt status
            log.warn("Thread was interrupted while fetching location history for user {}", userId, ie);
        } catch (Exception e) {
            log.error("Error fetching location history for user {}: {}", userId, e.getMessage(), e);
        }


        return Collections.emptyList(); // Return an empty list if no location data is found
    }
}