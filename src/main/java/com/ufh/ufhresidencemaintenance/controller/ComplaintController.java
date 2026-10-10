package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.Complaint;
import com.ufh.ufhresidencemaintenance.model.ComplaintCategory;
import com.ufh.ufhresidencemaintenance.model.ComplaintStatus;
import com.ufh.ufhresidencemaintenance.model.User;
import com.ufh.ufhresidencemaintenance.model.UserType;
import com.ufh.ufhresidencemaintenance.repository.ComplaintRepository;
import com.ufh.ufhresidencemaintenance.repository.FeedbackRepository;
import com.ufh.ufhresidencemaintenance.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class ComplaintController {

    private static final Path COMPLAINT_UPLOAD_DIR = Paths.get("uploads", "complaints");
    private static final long MAX_PHOTO_SIZE = 5L * 1024L * 1024L;

    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FeedbackRepository feedbackRepository;

    @GetMapping("/report-problem")
    public String showReportForm(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null) return "redirect:/login";

        // A one-time token prevents double-clicks, accidental resubmission and
        // browser refreshes from creating the same maintenance request twice.
        String complaintFormToken = UUID.randomUUID().toString();
        session.setAttribute("complaintFormToken", complaintFormToken);

        model.addAttribute("user", currentUser);
        model.addAttribute("complaintFormToken", complaintFormToken);
        return "report-problem";
    }

    @PostMapping("/report-problem")
    public String submitProblem(
            @RequestParam String description,
            @RequestParam ComplaintCategory category,
            @RequestParam String roomNumber,
            @RequestParam String complaintFormToken,
            @RequestParam(required = false, defaultValue = "false") boolean safetyConcern,
            @RequestParam(required = false) MultipartFile photo,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";

        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null) return "redirect:/login";

        // The checkbox is helpful, but the system also detects common safety
        // indicators so a student does not have to know the priority rules.
        boolean detectedSafetyConcern = safetyConcern || detectSafetyConcern(description);
        boolean isStaffOrAdmin = currentUser.getUserType() != UserType.STUDENT;
        boolean isHighSeverity = category.getSeverityWeight() >= 4 || detectedSafetyConcern;
        boolean residenceIsActive = currentUser.getResidence() != null
                && currentUser.getResidence().getZone() != null
                && currentUser.getResidence().getZone().isActive();

        if (!isStaffOrAdmin && !isHighSeverity && !residenceIsActive) {
            return "residence-not-active";
        }

        // Consume the form token after request validation but before saving any
        // file or complaint. This prevents duplicate submissions while still
        // allowing validation errors above to be corrected.
        synchronized (session) {
            String expectedToken = (String) session.getAttribute("complaintFormToken");
            if (expectedToken == null || !expectedToken.equals(complaintFormToken)) {
                return "redirect:/my-requests";
            }
            session.removeAttribute("complaintFormToken");
        }

        String savedPhotoName = null;
        if (photo != null && !photo.isEmpty()) {
            if (photo.getSize() > MAX_PHOTO_SIZE || photo.getContentType() == null
                    || !photo.getContentType().startsWith("image/")) {
                model.addAttribute("user", currentUser);
                model.addAttribute("error", "Please upload an image (JPG, PNG, etc.) no larger than 5 MB.");
                return "report-problem";
            }

            try {
                Files.createDirectories(COMPLAINT_UPLOAD_DIR);
                String original = StringUtils.cleanPath(photo.getOriginalFilename() == null ? "photo" : photo.getOriginalFilename());
                String extension = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) extension = original.substring(dot).toLowerCase();
                savedPhotoName = UUID.randomUUID() + extension;
                Files.copy(photo.getInputStream(), COMPLAINT_UPLOAD_DIR.resolve(savedPhotoName));
            } catch (IOException e) {
                session.setAttribute("complaintFormToken", complaintFormToken);
                model.addAttribute("user", currentUser);
                model.addAttribute("complaintFormToken", complaintFormToken);
                model.addAttribute("error", "The photo could not be saved. Please try again.");
                return "report-problem";
            }
        }

        Complaint complaint = new Complaint();
        complaint.setDescription(description);
        complaint.setRoomNumber(roomNumber);
        complaint.setCategory(category);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setDateLogged(java.time.LocalDateTime.now());
        complaint.setUser(currentUser);
        complaint.setPhotoPath(savedPhotoName);
        complaint.setSafetyConcern(detectedSafetyConcern);
        complaintRepository.save(complaint);

        model.addAttribute("priorityLevel", complaint.getPriorityLevel());
        model.addAttribute("safetyDetected", detectedSafetyConcern);
        return "report-success";
    }

    private boolean detectSafetyConcern(String description) {
        if (description == null || description.isBlank()) return false;
        String text = description.toLowerCase(java.util.Locale.ROOT);

        String[] directHazards = {
                "exposed wire", "exposed wiring", "sparking", "electric shock",
                "smoke", "fire", "gas leak", "flood", "flooding", "burst pipe",
                "broken door", "door broken", "broken lock", "lock broken",
                "broken gate", "gate broken", "cannot lock", "can't lock",
                "unsafe", "dangerous", "security risk", "intruder", "break in",
                "ceiling falling", "wall collapsing"
        };

        for (String hazard : directHazards) {
            if (text.contains(hazard)) return true;
        }

        // Catch combined reports such as “broken door and bulb”, where the
        // student may describe more than one maintenance category in one field.
        boolean securityIssue = text.contains("door") || text.contains("lock") || text.contains("gate");
        boolean electricalIssue = text.contains("bulb") || text.contains("light")
                || text.contains("socket") || text.contains("switch") || text.contains("electric");
        return securityIssue && electricalIssue;
    }

    @GetMapping("/complaints/photo/{id}")
    public ResponseEntity<Resource> getComplaintPhoto(@PathVariable Long id, HttpSession session) throws IOException {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User viewer = userRepository.findById(userId).orElse(null);
        Complaint complaint = complaintRepository.findById(id).orElse(null);
        if (viewer == null || complaint == null || complaint.getPhotoPath() == null) {
            return ResponseEntity.notFound().build();
        }

        boolean allowed = viewer.getUserType() == UserType.ADMIN
                || (complaint.getUser() != null && complaint.getUser().getId().equals(viewer.getId()));
        if (!allowed) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Path baseDir = COMPLAINT_UPLOAD_DIR.toAbsolutePath().normalize();
        Path file = baseDir.resolve(complaint.getPhotoPath()).normalize();
        if (!file.startsWith(baseDir)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!Files.exists(file)) return ResponseEntity.notFound().build();

        Resource resource = new UrlResource(file.toUri());
        MediaType mediaType = MediaTypeFactory.getMediaType(file.getFileName().toString()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    @GetMapping("/my-requests")
    public String showMyRequests(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";

        var complaints = complaintRepository.findByUserId(userId);
        Map<Long, Boolean> hasFeedback = new HashMap<>();
        for (Complaint complaint : complaints) {
            hasFeedback.put(complaint.getId(), feedbackRepository.existsByComplaintId(complaint.getId()));
        }

        model.addAttribute("complaints", complaints);
        model.addAttribute("hasFeedback", hasFeedback);
        return "my-requests";
    }
}
