package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.User;
import com.ufh.ufhresidencemaintenance.model.UserType;
import com.ufh.ufhresidencemaintenance.model.VerificationStatus;
import com.ufh.ufhresidencemaintenance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpSession;
import com.ufh.ufhresidencemaintenance.model.Residence;
import com.ufh.ufhresidencemaintenance.repository.ResidenceRepository;
import com.ufh.ufhresidencemaintenance.repository.ResidenceZoneRepository;
import com.ufh.ufhresidencemaintenance.repository.BulletinRepository;
import com.ufh.ufhresidencemaintenance.model.ResidenceZone;

@Controller
public class UserController {

    private static final Pattern STAFF_ID_PATTERN = Pattern.compile("^UFH/[0-9]{3}$");
    private static final Pattern STUDENT_NUMBER_PATTERN = Pattern.compile("^[0-9]{9}$");

    @Autowired private UserRepository userRepository;
    @Autowired private ResidenceRepository residenceRepository;
    @Autowired private ResidenceZoneRepository residenceZoneRepository;
    @Autowired private BulletinRepository bulletinRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("zones", residenceZoneRepository.findAll());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String fullName, @RequestParam String email, @RequestParam String password,
            @RequestParam UserType userType, @RequestParam(required = false) Long residenceId,
            @RequestParam(required = false) String staffId, @RequestParam(required = false) String studentNumber,
            @RequestParam(required = false) MultipartFile proofDocument, Model model) throws IOException {

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return registerError(model, "An account with this email already exists. Please sign in or use another email address.");
        }

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(normalizedEmail);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setUserType(userType);

        if (userType == UserType.STUDENT) {
            String normalizedStudent = studentNumber == null ? "" : studentNumber.trim();
            if (!STUDENT_NUMBER_PATTERN.matcher(normalizedStudent).matches()) {
                return registerError(model, "Student number must contain exactly 9 digits.");
            }
            if (userRepository.existsByStudentNumber(normalizedStudent)) {
                return registerError(model, "That student number is already registered.");
            }
            if (residenceId == null) return registerError(model, "Please select your residence.");
            Residence residence = residenceRepository.findById(residenceId).orElse(null);
            if (residence == null) return registerError(model, "Please select a valid residence.");
            newUser.setResidence(residence);
            newUser.setStudentNumber(normalizedStudent);
            newUser.setVerificationStatus(VerificationStatus.PENDING);

            if (proofDocument != null && !proofDocument.isEmpty()) {
                String uploadDir = "uploads/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                String fileName = UUID.randomUUID() + "_" + proofDocument.getOriginalFilename();
                Path filePath = Paths.get(uploadDir + fileName);
                Files.write(filePath, proofDocument.getBytes());
                newUser.setProofDocumentPath(filePath.toString());
            }
        } else if (userType == UserType.STAFF) {
            String normalizedStaff = staffId == null ? "" : staffId.trim().toUpperCase();
            if (!STAFF_ID_PATTERN.matcher(normalizedStaff).matches()) {
                return registerError(model, "Staff ID must use the format UFH/123 (UFH followed by exactly 3 digits).");
            }
            if (userRepository.existsByStaffId(normalizedStaff)) {
                return registerError(model, "That Staff ID is already registered.");
            }
            newUser.setStaffId(normalizedStaff);
            newUser.setVerificationStatus(VerificationStatus.APPROVED);
        }

        userRepository.save(newUser);
        return "register-success";
    }

    private String registerError(Model model, String message) {
        model.addAttribute("zones", residenceZoneRepository.findAll());
        model.addAttribute("error", message);
        return "register";
    }

    @GetMapping("/login")
    public String showLoginForm() { return "login"; }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email, @RequestParam String password, Model model, HttpSession session) {
        email = email == null ? "" : email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            session.setAttribute("loggedInUser", userOpt.get().getId());
            if (userOpt.get().getUserType() == UserType.ADMIN) return "redirect:/admin/dashboard";
            return "redirect:/dashboard";
        }
        model.addAttribute("error", "Invalid email or password");
        return "login";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(HttpSession session) {
        return session.getAttribute("loggedInUser") == null ? "redirect:/login" : "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "redirect:/login";
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("error", "Your current password is incorrect.");
            return "change-password";
        }
        if (newPassword == null || newPassword.length() < 8) {
            model.addAttribute("error", "New password must be at least 8 characters long.");
            return "change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "The new passwords do not match.");
            return "change-password";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return user.getUserType() == UserType.ADMIN
                ? "redirect:/admin/dashboard?passwordChanged=true"
                : "redirect:/dashboard?passwordChanged=true";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, @RequestParam(required = false) String studentNumber,
                                 @RequestParam(required = false) String staffId, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, Model model) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) { model.addAttribute("error", "No account was found for that email."); return "forgot-password"; }
        boolean identityMatches = user.getUserType() == UserType.STUDENT
                ? user.getStudentNumber() != null && user.getStudentNumber().equals(studentNumber == null ? "" : studentNumber.trim())
                : user.getStaffId() != null && user.getStaffId().equals((staffId == null ? "" : staffId.trim()).toUpperCase());
        if (!identityMatches) { model.addAttribute("error", "The account details could not be verified."); return "forgot-password"; }
        if (newPassword == null || newPassword.length() < 8) { model.addAttribute("error", "New password must be at least 8 characters long."); return "forgot-password"; }
        if (!newPassword.equals(confirmPassword)) { model.addAttribute("error", "The new passwords do not match."); return "forgot-password"; }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return "redirect:/login?reset=true";
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";
        User currentUser = userRepository.findById(userId).orElse(null);
        model.addAttribute("user", currentUser);
        ResidenceZone activeZone = residenceZoneRepository.findByActiveTrue().orElse(null);
        model.addAttribute("activeZone", activeZone);
        model.addAttribute("bulletins", bulletinRepository.findTop3ByActiveTrueOrderByCreatedAtDesc());
        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) { session.invalidate(); return "redirect:/login?loggedout=true"; }
}
