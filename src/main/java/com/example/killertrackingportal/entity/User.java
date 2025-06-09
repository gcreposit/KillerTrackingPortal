package com.example.killertrackingportal.entity;


import com.google.cloud.Timestamp;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "killer_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private boolean tracking;
    private String docId;
    private String latestLocation;
    private boolean showLastKnownLocation = false;

    public boolean isShowLastKnownLocation() {
        return showLastKnownLocation;
    }

    public void setShowLastKnownLocation(boolean showLastKnownLocation) {
        this.showLastKnownLocation = showLastKnownLocation;
    }

    public String getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(String lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    private String lastActiveAt;

    private String oldestLocation; // Assuming this is a field of type Location, adjust as necessary

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")  // Foreign key for the Location table
    private List<Location> locationHistory;

    private Timestamp createdAt;  // Add createdAt field
    private Boolean admin;     // Add admin field


    @Column(name = "bus_no")
    private String busNo;   // This will hold the bus_no field value
    public String getBusNo() {
        return busNo;
    }

    public void setBusNo(String busNo) {
        this.busNo = busNo;
    }



}
