package com.ufh.ufhresidencemaintenance.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_active_status", columnList = "archived,status")
})
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

    @Column(length = 2000)
    private String adminResponse;

    private LocalDateTime responseDate;

    /** Indicates that the reported issue presents a direct safety concern. */
    @Column(nullable = false)
    private boolean safetyConcern = false;

    @Column(length = 500)
    private String photoPath;

    /**
     * Soft-delete/archive fields. Archived complaints remain in the database
     * so administrators can retrieve them for audit/reporting purposes.
     */
    @Column(nullable = false)
    private boolean archived = false;

    private LocalDateTime archivedAt;

    @ManyToOne
    @JoinColumn(name = "archived_by_admin_id")
    private User archivedByAdmin;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public LocalDateTime getDateLogged() { return dateLogged; }
    public void setDateLogged(LocalDateTime dateLogged) { this.dateLogged = dateLogged; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public LocalDateTime getSeenAt() { return seenAt; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }

    public Technician getTechnician() { return technician; }
    public void setTechnician(Technician technician) { this.technician = technician; }

    /**
     * Priority increases with waiting time while a complaint is active.
     * Once resolved, the score is frozen at the value it had at resolution.
     */
    public double getPriorityScore() {
        if (category == null) return 0.0;
        double base = category.getSeverityWeight();
        if (safetyConcern) base += 2.0;
        if (dateLogged == null) return base;

        LocalDateTime end = status == ComplaintStatus.RESOLVED
                ? (resolvedAt != null ? resolvedAt : dateLogged)
                : LocalDateTime.now();

        long hours = Math.max(0, Duration.between(dateLogged, end).toHours());
        return base + (hours / 24.0);
    }

    /**
     * Human-readable priority bucket for administrators.
     * The category severity is combined with waiting time through priorityScore.
     */
    public String getPriorityLevel() {
        double score = getPriorityScore();
        if (score >= 4.0) return "HIGH";
        if (score >= 2.0) return "MEDIUM";
        return "LOW";
    }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public User getOpenedByAdmin() { return openedByAdmin; }
    public void setOpenedByAdmin(User openedByAdmin) { this.openedByAdmin = openedByAdmin; }

    public User getLastUpdatedByAdmin() { return lastUpdatedByAdmin; }
    public void setLastUpdatedByAdmin(User lastUpdatedByAdmin) { this.lastUpdatedByAdmin = lastUpdatedByAdmin; }

    public boolean isOverdue() {
        if (dateLogged == null || status == ComplaintStatus.RESOLVED || archived) return false;
        long hoursSinceLogged = Duration.between(dateLogged, LocalDateTime.now()).toHours();
        return hoursSinceLogged > 72;
    }

    public String getDisplayStatus() {
        if (archived) return "Archived";
        if (status == ComplaintStatus.RESOLVED) return "Resolved";
        if (seenAt == null) return "Submitted";
        if (status == ComplaintStatus.OPEN) return "Waiting List";
        return "In Progress";
    }

    /**
     * Elapsed hours from submission to resolution, or to now if still active.
     */
    public double getElapsedHours() {
        if (dateLogged == null) return 0.0;
        LocalDateTime end = (status == ComplaintStatus.RESOLVED && resolvedAt != null)
                ? resolvedAt
                : LocalDateTime.now();
        return Math.max(0.0, Duration.between(dateLogged, end).toMinutes() / 60.0);
    }

    /**
     * Compact administrator-friendly display of how long the complaint has been open.
     * Examples: 2h 14m, 1d 3h 08m, 3d 0h 42m.
     */
    public String getElapsedDisplay() {
        if (dateLogged == null) return "Not available";
        LocalDateTime end = (status == ComplaintStatus.RESOLVED && resolvedAt != null)
                ? resolvedAt : LocalDateTime.now();
        long totalMinutes = Math.max(0, Duration.between(dateLogged, end).toMinutes());
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return days + (days == 1 ? " day" : " days") + ", "
                    + hours + (hours == 1 ? " hour" : " hours");
        }
        if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours") + ", "
                    + minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }

    /** Explains why the complaint is being treated as a safety-related issue. */
    public String getSafetyReason() {
        if (!safetyConcern) return "";
        return "Safety concern reported or detected; this can increase priority.";
    }

    public boolean isSafetyConcern() { return safetyConcern; }
    public void setSafetyConcern(boolean safetyConcern) { this.safetyConcern = safetyConcern; }

    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }

    public LocalDateTime getResponseDate() { return responseDate; }
    public void setResponseDate(LocalDateTime responseDate) { this.responseDate = responseDate; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    public User getArchivedByAdmin() { return archivedByAdmin; }
    public void setArchivedByAdmin(User archivedByAdmin) { this.archivedByAdmin = archivedByAdmin; }
}
