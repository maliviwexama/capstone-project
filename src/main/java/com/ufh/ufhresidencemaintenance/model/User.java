package com.ufh.ufhresidencemaintenance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserType userType;
    @ManyToOne
    @JoinColumn(name = "residence_id")
    private Residence residence;
    private String staffId;
    private String studentNumber;
    private String proofDocumentPath;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private java.time.LocalDateTime verificationReviewedAt;

    @Column(length = 500)
    private String verificationReviewMessage;

    @ManyToOne
    @JoinColumn(name = "verification_reviewed_by_admin_id")
    private User verificationReviewedByAdmin;

    // Account protection: five incorrect logins trigger a temporary lock.
    private int failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;

    // Password reset protection: at most three failed identity checks in a 30-minute window.
    private int passwordResetAttempts = 0;
    private LocalDateTime passwordResetWindowStart;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }
    public Residence getResidence() {
        return residence;
    }

    public void setResidence(Residence residence) {
        this.residence = residence;
    }
    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }
    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getProofDocumentPath() {
        return proofDocumentPath;
    }

    public void setProofDocumentPath(String proofDocumentPath) {
        this.proofDocumentPath = proofDocumentPath;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public java.time.LocalDateTime getVerificationReviewedAt() {
        return verificationReviewedAt;
    }

    public void setVerificationReviewedAt(java.time.LocalDateTime verificationReviewedAt) {
        this.verificationReviewedAt = verificationReviewedAt;
    }

    public String getVerificationReviewMessage() {
        return verificationReviewMessage;
    }

    public void setVerificationReviewMessage(String verificationReviewMessage) {
        this.verificationReviewMessage = verificationReviewMessage;
    }

    public User getVerificationReviewedByAdmin() {
        return verificationReviewedByAdmin;
    }

    public void setVerificationReviewedByAdmin(User verificationReviewedByAdmin) {
        this.verificationReviewedByAdmin = verificationReviewedByAdmin;
    }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public int getPasswordResetAttempts() { return passwordResetAttempts; }
    public void setPasswordResetAttempts(int passwordResetAttempts) { this.passwordResetAttempts = passwordResetAttempts; }
    public LocalDateTime getPasswordResetWindowStart() { return passwordResetWindowStart; }
    public void setPasswordResetWindowStart(LocalDateTime passwordResetWindowStart) { this.passwordResetWindowStart = passwordResetWindowStart; }
}
