package com.ufh.ufhresidencemaintenance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    private LocalDateTime dateLogged;
    private LocalDateTime seenAt;
    private LocalDateTime resolvedAt;

    @ManyToOne
    @JoinColumn(name = "opened_by_admin_id")
    private User openedByAdmin;

    @ManyToOne
    @JoinColumn(name = "last_updated_by_admin_id")
    private User lastUpdatedByAdmin;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "technician_id")
    private Technician technician;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public LocalDateTime getDateLogged() {
        return dateLogged;
    }

    public void setDateLogged(LocalDateTime dateLogged) {
        this.dateLogged = dateLogged;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
    public LocalDateTime getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(LocalDateTime seenAt) {
        this.seenAt = seenAt;
    }

    public Technician getTechnician() {
        return technician;
    }

    public void setTechnician(Technician technician) {
        this.technician = technician;
    }
    public double getPriorityScore() {
        if (dateLogged == null) {
            return category.getSeverityWeight();
        }
        long hoursSinceLogged = java.time.Duration.between(dateLogged, java.time.LocalDateTime.now()).toHours();
        double decayFactor = 24.0;
        return category.getSeverityWeight() + (hoursSinceLogged / decayFactor);
    }
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    public User getOpenedByAdmin() {
        return openedByAdmin;
    }

    public void setOpenedByAdmin(User openedByAdmin) {
        this.openedByAdmin = openedByAdmin;
    }

    public User getLastUpdatedByAdmin() {
        return lastUpdatedByAdmin;
    }

    public void setLastUpdatedByAdmin(User lastUpdatedByAdmin) {
        this.lastUpdatedByAdmin = lastUpdatedByAdmin;
    }
    public boolean isOverdue() {
        if (dateLogged == null || status == ComplaintStatus.RESOLVED) {
            return false;
        }
        long hoursSinceLogged = java.time.Duration.between(dateLogged, java.time.LocalDateTime.now()).toHours();
        return hoursSinceLogged > 72;
    }
    public String getDisplayStatus() {
        if (status == ComplaintStatus.RESOLVED) {
            return "Resolved";
        }
        if (seenAt == null) {
            return "Submitted";
        }
        if (status == ComplaintStatus.OPEN) {
            return "Waiting List";
        }
        return "In Progress";
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
