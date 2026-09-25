package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.*;
import com.ufh.ufhresidencemaintenance.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired private ResidenceZoneRepository residenceZoneRepository;
    @Autowired private ResidenceRepository residenceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private TechnicianRepository technicianRepository;
    @Autowired private FeedbackRepository feedbackRepository;

    private User getAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return null;
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getUserType() == UserType.ADMIN ? user : null;
    }

    @GetMapping("/admin/residences")
    public String listZones(Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        model.addAttribute("zones", residenceZoneRepository.findAll());
        return "admin-residences";
    }

    @PostMapping("/admin/residences/activate")
    public String activateZone(@RequestParam Long zoneId, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        List<ResidenceZone> all = residenceZoneRepository.findAll();
        for (ResidenceZone z : all) z.setActive(z.getId().equals(zoneId));
        residenceZoneRepository.saveAll(all);
        return "redirect:/admin/residences";
    }

    @PostMapping("/admin/residences/manager-phone")
    public String updateResidenceManagerPhone(@RequestParam Long residenceId,
                                               @RequestParam(required = false) String managerPhone,
                                               HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        Residence residence = residenceRepository.findById(residenceId).orElse(null);
        if (residence != null) {
            residence.setManagerPhone(managerPhone == null ? null : managerPhone.trim());
            residenceRepository.save(residence);
        }
        return "redirect:/admin/residences";
    }

    @GetMapping("/admin/complaints")
    public String listComplaints(@RequestParam(defaultValue = "priority") String sort,
                                 Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";

        List<Complaint> complaints = complaintRepository.findByArchivedFalseAndStatusNot(ComplaintStatus.RESOLVED);

        Comparator<Complaint> comparator;
        switch (sort) {
            case "oldest" -> comparator = Comparator.comparing(Complaint::getDateLogged,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "newest" -> comparator = Comparator.comparing(Complaint::getDateLogged,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case "overdue" -> comparator = Comparator.comparing(Complaint::isOverdue).reversed()
                    .thenComparing(Complaint::getDateLogged, Comparator.nullsLast(Comparator.naturalOrder()));
            case "high-medium-low" -> comparator = Comparator
                    .comparingInt((Complaint c) -> priorityRank(c.getPriorityLevel())).reversed()
                    .thenComparing(Comparator.comparing(Complaint::getPriorityScore).reversed());
            default -> comparator = Comparator.comparing(Complaint::isOverdue).reversed()
                    .thenComparing(Comparator.comparing(Complaint::getPriorityScore).reversed());
        }

        complaints.sort(comparator);
        model.addAttribute("complaints", complaints);
        model.addAttribute("technicians", technicianRepository.findAll());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pendingVerificationCount", userRepository.findByVerificationStatus(VerificationStatus.PENDING).size());
        return "admin-complaints";
    }

    private int priorityRank(String level) {
        return switch (level) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    @GetMapping("/admin/resolved-complaints")
    public String resolvedComplaints(Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        model.addAttribute("complaints",
                complaintRepository.findByArchivedFalseAndStatusOrderByResolvedAtDesc(ComplaintStatus.RESOLVED));
        return "admin-resolved-complaints";
    }

    @PostMapping("/admin/resolved-complaints/{id}/clear")
    public String clearResolvedComplaint(@PathVariable Long id, HttpSession session) {
        User admin = getAdmin(session);
        if (admin == null) return "redirect:/login";

        Complaint complaint = complaintRepository.findById(id).orElse(null);
        if (complaint != null && complaint.getStatus() == ComplaintStatus.RESOLVED && !complaint.isArchived()) {
            // Soft delete: retain the record, feedback and photo for audit/retrieval.
            complaint.setArchived(true);
            complaint.setArchivedAt(LocalDateTime.now());
            complaint.setArchivedByAdmin(admin);
            complaintRepository.save(complaint);
        }
        return "redirect:/admin/resolved-complaints";
    }

    @GetMapping("/admin/archived-complaints")
    public String archivedComplaints(Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        model.addAttribute("complaints", complaintRepository.findByArchivedTrueOrderByArchivedAtDesc());
        return "admin-archived-complaints";
    }

    @PostMapping("/admin/archived-complaints/{id}/restore")
    public String restoreComplaint(@PathVariable Long id, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        Complaint complaint = complaintRepository.findById(id).orElse(null);
        if (complaint != null && complaint.isArchived()) {
            complaint.setArchived(false);
            complaint.setArchivedAt(null);
            complaint.setArchivedByAdmin(null);
            complaintRepository.save(complaint);
        }
        return "redirect:/admin/archived-complaints";
    }

    @PostMapping("/admin/complaints/assign")
    public String assignComplaint(@RequestParam Long complaintId,
                                  @RequestParam Long technicianId,
                                  @RequestParam ComplaintStatus status,
                                  HttpSession session) {
        User currentUser = getAdmin(session);
        if (currentUser == null) return "redirect:/login";

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        Technician technician = technicianRepository.findById(technicianId).orElse(null);

        if (complaint != null && !complaint.isArchived()) {
            if (complaint.getUser() != null && complaint.getUser().getUserType() == UserType.STUDENT
                    && complaint.getUser().getVerificationStatus() != VerificationStatus.APPROVED) {
                return "redirect:/admin/complaints?verificationRequired=true";
            }
            ComplaintStatus previousStatus = complaint.getStatus();
            complaint.setTechnician(technician);
            complaint.setStatus(status);

            if (complaint.getSeenAt() == null) complaint.setSeenAt(LocalDateTime.now());
            if (complaint.getOpenedByAdmin() == null) complaint.setOpenedByAdmin(currentUser);
            complaint.setLastUpdatedByAdmin(currentUser);

            if (status == ComplaintStatus.RESOLVED && previousStatus != ComplaintStatus.RESOLVED) {
                complaint.setResolvedAt(LocalDateTime.now());
            } else if (status != ComplaintStatus.RESOLVED) {
                complaint.setResolvedAt(null);
            }
            complaintRepository.save(complaint);
        }

        return "redirect:/admin/complaints";
    }

    @PostMapping("/admin/complaints/respond")
    public String respondToComplaint(@RequestParam Long complaintId,
                                     @RequestParam String response,
                                     HttpSession session) {
        User currentUser = getAdmin(session);
        if (currentUser == null) return "redirect:/login";

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint != null && !complaint.isArchived() && response != null && !response.isBlank()) {
            if (complaint.getUser() != null && complaint.getUser().getUserType() == UserType.STUDENT
                    && complaint.getUser().getVerificationStatus() != VerificationStatus.APPROVED) {
                return "redirect:/admin/complaints?verificationRequired=true";
            }
            complaint.setAdminResponse(response.trim());
            complaint.setResponseDate(LocalDateTime.now());
            complaint.setLastUpdatedByAdmin(currentUser);
            if (complaint.getSeenAt() == null) complaint.setSeenAt(LocalDateTime.now());
            if (complaint.getOpenedByAdmin() == null) complaint.setOpenedByAdmin(currentUser);
            complaintRepository.save(complaint);
        }
        return "redirect:/admin/complaints";
    }

    @GetMapping("/admin/complaints/export")
    public ResponseEntity<byte[]> exportActiveComplaints(HttpSession session) {
        if (getAdmin(session) == null) return ResponseEntity.status(403).build();

        List<Complaint> complaints = complaintRepository.findByArchivedFalseAndStatusNot(ComplaintStatus.RESOLVED);
        String csv = buildComplaintCsv(complaints);
        return csvResponse(csv, "ufh-active-maintenance-complaints.csv");
    }

    @GetMapping("/admin/resolved-complaints/export")
    public ResponseEntity<byte[]> exportResolvedComplaints(HttpSession session) {
        if (getAdmin(session) == null) return ResponseEntity.status(403).build();

        List<Complaint> complaints =
                complaintRepository.findByArchivedFalseAndStatusOrderByResolvedAtDesc(ComplaintStatus.RESOLVED);
        String csv = buildComplaintCsv(complaints);
        return csvResponse(csv, "ufh-resolved-maintenance-complaints.csv");
    }

    @GetMapping("/admin/archived-complaints/export")
    public ResponseEntity<byte[]> exportArchivedComplaints(HttpSession session) {
        if (getAdmin(session) == null) return ResponseEntity.status(403).build();

        String csv = buildComplaintCsv(complaintRepository.findByArchivedTrueOrderByArchivedAtDesc());
        return csvResponse(csv, "ufh-archived-maintenance-complaints.csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private String buildComplaintCsv(List<Complaint> complaints) {
        StringBuilder out = new StringBuilder();
        out.append("ID,Student,Student Verification,Room,Residence,Category,Priority Level,Priority Score,Status,")
                .append("Date Logged,Elapsed Hours,Resolved At,Technician/Attended By,Admin Response,Archived,Archived At\n");

        for (Complaint c : complaints) {
            String student = c.getUser() != null ? c.getUser().getFullName() : "";
            String verification = c.getUser() != null && c.getUser().getVerificationStatus() != null
                    ? c.getUser().getVerificationStatus().name() : "";
            String room = c.getRoomNumber() == null ? "" : c.getRoomNumber();
            String residence = c.getUser() != null && c.getUser().getResidence() != null
                    ? c.getUser().getResidence().getName() : "";
            String category = c.getCategory() == null ? "" : c.getCategory().name();
            String technician = c.getTechnician() == null ? "" : c.getTechnician().getName();
            String resolvedAt = c.getResolvedAt() == null ? "" : c.getResolvedAt().toString();
            String archivedAt = c.getArchivedAt() == null ? "" : c.getArchivedAt().toString();

            out.append(c.getId()).append(',')
                    .append(csv(student)).append(',')
                    .append(csv(verification)).append(',')
                    .append(csv(room)).append(',')
                    .append(csv(residence)).append(',')
                    .append(csv(category)).append(',')
                    .append(c.getPriorityLevel()).append(',')
                    .append(String.format(Locale.US, "%.2f", c.getPriorityScore())).append(',')
                    .append(c.getStatus()).append(',')
                    .append(csv(String.valueOf(c.getDateLogged()))).append(',')
                    .append(String.format(Locale.US, "%.2f", c.getElapsedHours())).append(',')
                    .append(csv(resolvedAt)).append(',')
                    .append(csv(technician)).append(',')
                    .append(csv(c.getAdminResponse())).append(',')
                    .append(c.isArchived()).append(',')
                    .append(csv(archivedAt)).append('\n');
        }
        return out.toString();
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }

    @GetMapping("/admin/members")
    public String addMemberForm(Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        model.addAttribute("residences", residenceZoneRepository.findAll());
        return "admin-add-member";
    }

    @PostMapping("/admin/members")
    public String addMember(@RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam String password,
                            @RequestParam UserType userType,
                            @RequestParam(required = false) Long residenceId,
                            @RequestParam(required = false) String studentNumber,
                            @RequestParam(required = false) String staffId,
                            Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            model.addAttribute("error", "An account with that email already exists.");
            model.addAttribute("residences", residenceZoneRepository.findAll());
            return "admin-add-member";
        }

        if (userType == UserType.STUDENT) {
            String sn = studentNumber == null ? "" : studentNumber.trim();
            if (!java.util.regex.Pattern.matches("^[0-9]{9}$", sn)) {
                model.addAttribute("error", "Student number must contain exactly 9 digits.");
                model.addAttribute("residences", residenceZoneRepository.findAll());
                return "admin-add-member";
            }
            if (userRepository.existsByStudentNumber(sn)) {
                model.addAttribute("error", "That student number is already registered.");
                model.addAttribute("residences", residenceZoneRepository.findAll());
                return "admin-add-member";
            }
            studentNumber = sn;
        } else {
            String sid = staffId == null ? "" : staffId.trim().toUpperCase();
            if (!java.util.regex.Pattern.matches("^UFH/[0-9]{3}$", sid)) {
                model.addAttribute("error", "Staff ID must use the format UFH/123 (UFH followed by exactly 3 digits).");
                model.addAttribute("residences", residenceZoneRepository.findAll());
                return "admin-add-member";
            }
            if (userRepository.existsByStaffId(sid)) {
                model.addAttribute("error", "That Staff ID is already registered.");
                model.addAttribute("residences", residenceZoneRepository.findAll());
                return "admin-add-member";
            }
            staffId = sid;
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(normalizedEmail);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setUserType(userType);

        if (userType == UserType.STUDENT) {
            user.setStudentNumber(studentNumber);
            user.setVerificationStatus(VerificationStatus.APPROVED);
            user.setVerificationReviewedAt(LocalDateTime.now());
            user.setVerificationReviewedByAdmin(getAdmin(session));
            if (residenceId != null) user.setResidence(residenceRepository.findById(residenceId).orElse(null));
        } else {
            user.setStaffId(staffId);
            user.setVerificationStatus(VerificationStatus.APPROVED);
        }

        userRepository.save(user);
        return "redirect:/admin/members?success=true";
    }

    @GetMapping("/admin/feedback")
    public String listFeedback(Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        model.addAttribute("feedbackList", feedbackRepository.findAll());
        return "admin-feedback";
    }

    @GetMapping("/admin/statistics")
    public String showStatistics(Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";

        List<Complaint> allComplaints = complaintRepository.findAll();
        Map<ComplaintCategory, Long> categoryCounts = new LinkedHashMap<>();
        Map<ComplaintCategory, Double> totalResolutionHours = new LinkedHashMap<>();
        Map<ComplaintCategory, Integer> resolvedCounts = new LinkedHashMap<>();
        Map<ComplaintCategory, Double> activeElapsedHours = new LinkedHashMap<>();
        Map<ComplaintCategory, Integer> activeCounts = new LinkedHashMap<>();

        for (Complaint c : allComplaints) {
            if (c.getCategory() == null) continue;
            categoryCounts.merge(c.getCategory(), 1L, Long::sum);

            if (c.getResolvedAt() != null && c.getDateLogged() != null) {
                double hours = Duration.between(c.getDateLogged(), c.getResolvedAt()).toMinutes() / 60.0;
                totalResolutionHours.merge(c.getCategory(), hours, Double::sum);
                resolvedCounts.merge(c.getCategory(), 1, Integer::sum);
            }

            if (!c.isArchived() && c.getStatus() != ComplaintStatus.RESOLVED) {
                activeElapsedHours.merge(c.getCategory(), c.getElapsedHours(), Double::sum);
                activeCounts.merge(c.getCategory(), 1, Integer::sum);
            }
        }

        Map<ComplaintCategory, Double> avgResolutionHours = new LinkedHashMap<>();
        for (ComplaintCategory cat : resolvedCounts.keySet()) {
            avgResolutionHours.put(cat, totalResolutionHours.get(cat) / resolvedCounts.get(cat));
        }

        Map<ComplaintCategory, Double> avgActiveHours = new LinkedHashMap<>();
        for (ComplaintCategory cat : activeCounts.keySet()) {
            avgActiveHours.put(cat, activeElapsedHours.get(cat) / activeCounts.get(cat));
        }

        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("avgResolutionHours", avgResolutionHours);
        model.addAttribute("resolvedCounts", resolvedCounts);
        model.addAttribute("avgActiveHours", avgActiveHours);
        model.addAttribute("totalComplaints", allComplaints.size());
        model.addAttribute("activeCount", complaintRepository.countByArchivedFalseAndStatusNot(ComplaintStatus.RESOLVED));
        model.addAttribute("resolvedCount", complaintRepository.countByArchivedFalseAndStatus(ComplaintStatus.RESOLVED));
        model.addAttribute("archivedCount", complaintRepository.findByArchivedTrueOrderByArchivedAtDesc().size());

        return "admin-statistics";
    }

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(Model model, HttpSession session) {
        User currentUser = getAdmin(session);
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("user", currentUser);
        return "admin-dashboard";
    }

    @GetMapping("/admin/verifications")
    public String listPendingVerifications(Model model, HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        model.addAttribute("pendingUsers", userRepository.findByVerificationStatus(VerificationStatus.PENDING));
        List<User> verificationHistory = userRepository.findAll().stream()
                .filter(u -> u.getUserType() == UserType.STUDENT && u.getVerificationStatus() != null
                        && u.getVerificationStatus() != VerificationStatus.PENDING)
                .sorted(Comparator.comparing(User::getVerificationReviewedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        model.addAttribute("verificationHistory", verificationHistory);
        return "admin-verifications";
    }

    @PostMapping("/admin/verifications/review")
    public String reviewVerification(@RequestParam Long userId,
                                      @RequestParam VerificationStatus decision,
                                      HttpSession session) {
        if (getAdmin(session) == null) return "redirect:/login";
        User admin = getAdmin(session);
        User student = userRepository.findById(userId).orElse(null);
        if (admin != null && student != null) {
            student.setVerificationStatus(decision);
            student.setVerificationReviewedAt(LocalDateTime.now());
            student.setVerificationReviewedByAdmin(admin);
            userRepository.save(student);
        }
        return "redirect:/admin/verifications";
    }

    @GetMapping("/admin/verifications/document")
    public ResponseEntity<org.springframework.core.io.Resource> viewDocument(
            @RequestParam Long userId, @RequestParam(defaultValue = "false") boolean download, HttpSession session) throws java.io.IOException {
        if (getAdmin(session) == null) return ResponseEntity.status(403).build();

        User student = userRepository.findById(userId).orElse(null);
        if (student == null || student.getProofDocumentPath() == null) return ResponseEntity.notFound().build();

        java.io.File file = new java.io.File(student.getProofDocumentPath());
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);

        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) contentType = "application/octet-stream";

        String disposition = download ? "attachment" : "inline";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
