package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.ResidenceZone;
import com.ufh.ufhresidencemaintenance.repository.ResidenceZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.ufh.ufhresidencemaintenance.model.User;
import com.ufh.ufhresidencemaintenance.model.UserType;
import com.ufh.ufhresidencemaintenance.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import com.ufh.ufhresidencemaintenance.model.Complaint;
import com.ufh.ufhresidencemaintenance.model.ComplaintStatus;
import com.ufh.ufhresidencemaintenance.model.Technician;
import com.ufh.ufhresidencemaintenance.repository.ComplaintRepository;
import com.ufh.ufhresidencemaintenance.repository.TechnicianRepository;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import com.ufh.ufhresidencemaintenance.model.ComplaintCategory;
import com.ufh.ufhresidencemaintenance.model.VerificationStatus;

import java.util.List;

@Controller
public class AdminController {

    @Autowired
    private ResidenceZoneRepository residenceZoneRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @GetMapping("/admin/residences")
    public String listZones(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return "redirect:/dashboard";
        }

        model.addAttribute("zones", residenceZoneRepository.findAll());
        return "admin-residences";
    }

    @PostMapping("/admin/residences/activate")
    public String activateZone(@RequestParam Long zoneId, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return "redirect:/dashboard";
        }

        List<ResidenceZone> all = residenceZoneRepository.findAll();
        for (ResidenceZone z : all) {
            z.setActive(z.getId().equals(zoneId));
        }
        residenceZoneRepository.saveAll(all);
        return "redirect:/admin/residences";
    }
    @GetMapping("/admin/complaints")
    public String listComplaints(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return "redirect:/dashboard";
        }
        List<Complaint> complaints = complaintRepository.findAll();
        complaints.sort((a, b) -> {
            if (a.isOverdue() && !b.isOverdue()) return -1;
            if (!a.isOverdue() && b.isOverdue()) return 1;
            if (a.isOverdue() && b.isOverdue()) return a.getDateLogged().compareTo(b.getDateLogged());
            return Double.compare(b.getPriorityScore(), a.getPriorityScore());
        });

        model.addAttribute("complaints", complaintRepository.findAll());
        model.addAttribute("technicians", technicianRepository.findAll());
        return "admin-complaints";
    }

    @PostMapping("/admin/complaints/assign")
    public String assignComplaint(
            @RequestParam Long complaintId,
            @RequestParam Long technicianId,
            @RequestParam ComplaintStatus status,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return "redirect:/dashboard";
        }

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        Technician technician = technicianRepository.findById(technicianId).orElse(null);

        if (complaint != null) {
            complaint.setTechnician(technician);
            complaint.setStatus(status);
            if (complaint.getSeenAt() == null) {
                complaint.setSeenAt(LocalDateTime.now());
            }
            if (complaint.getOpenedByAdmin() == null) {
                complaint.setOpenedByAdmin(currentUser);
            }
            complaint.setLastUpdatedByAdmin(currentUser);
            if (status == ComplaintStatus.RESOLVED && complaint.getResolvedAt() == null) {
                complaint.setResolvedAt(LocalDateTime.now());
            }
            complaintRepository.save(complaint);
        }

        return "redirect:/admin/complaints";
    }
    @GetMapping("/admin/statistics")
    public String showStatistics(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return "redirect:/dashboard";
        }

        List<Complaint> allComplaints = complaintRepository.findAll();

        Map<ComplaintCategory, Long> categoryCounts = new HashMap<>();
        Map<ComplaintCategory, Double> totalResolutionHours = new HashMap<>();
        Map<ComplaintCategory, Integer> resolvedCounts = new HashMap<>();

        for (Complaint c : allComplaints) {
            categoryCounts.merge(c.getCategory(), 1L, Long::sum);

            if (c.getResolvedAt() != null && c.getDateLogged() != null) {
                double hours = Duration.between(c.getDateLogged(), c.getResolvedAt()).toMinutes() / 60.0;
                totalResolutionHours.merge(c.getCategory(), hours, Double::sum);
                resolvedCounts.merge(c.getCategory(), 1, Integer::sum);
            }
        }

        Map<ComplaintCategory, Double> avgResolutionHours = new HashMap<>();
        for (ComplaintCategory cat : resolvedCounts.keySet()) {
            avgResolutionHours.put(cat, totalResolutionHours.get(cat) / resolvedCounts.get(cat));
        }

        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("avgResolutionHours", avgResolutionHours);
        model.addAttribute("totalComplaints", allComplaints.size());

        return "admin-statistics";
    }
    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return "redirect:/dashboard";
        }

        model.addAttribute("user", currentUser);
        return "admin-dashboard";
    }
    @GetMapping("/admin/verifications")
    public String listPendingVerifications(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return "redirect:/dashboard";
        }

        model.addAttribute("pendingUsers", userRepository.findByVerificationStatus(VerificationStatus.PENDING));
        return "admin-verifications";
    }

    @PostMapping("/admin/verifications/review")
    public String reviewVerification(
            @RequestParam Long userId,
            @RequestParam VerificationStatus decision,
            HttpSession session) {

        Long adminId = (Long) session.getAttribute("loggedInUser");
        if (adminId == null) {
            return "redirect:/login";
        }
        User currentUser = userRepository.findById(adminId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return "redirect:/dashboard";
        }

        User student = userRepository.findById(userId).orElse(null);
        if (student != null) {
            student.setVerificationStatus(decision);
            userRepository.save(student);
        }

        return "redirect:/admin/verifications";
    }
    @GetMapping("/admin/verifications/document")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> viewDocument(
            @RequestParam Long userId, HttpSession session) throws java.io.IOException {

        Long adminId = (Long) session.getAttribute("loggedInUser");
        User currentUser = userRepository.findById(adminId).orElse(null);
        if (currentUser == null || currentUser.getUserType() != UserType.ADMIN) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }

        User student = userRepository.findById(userId).orElse(null);
        if (student == null || student.getProofDocumentPath() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        java.io.File file = new java.io.File(student.getProofDocumentPath());
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);

        String contentType = java.nio.file.Files.probeContentType(file.toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
