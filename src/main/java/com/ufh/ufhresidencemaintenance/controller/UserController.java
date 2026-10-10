package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.Residence;
import com.ufh.ufhresidencemaintenance.model.ResidenceZone;
import com.ufh.ufhresidencemaintenance.model.User;
import com.ufh.ufhresidencemaintenance.model.UserType;
import com.ufh.ufhresidencemaintenance.model.VerificationStatus;
import com.ufh.ufhresidencemaintenance.repository.BulletinRepository;
import com.ufh.ufhresidencemaintenance.repository.ResidenceRepository;
import com.ufh.ufhresidencemaintenance.repository.ResidenceZoneRepository;
import com.ufh.ufhresidencemaintenance.repository.UserRepository;
import com.ufh.ufhresidencemaintenance.util.LoginSecurityPolicy;
import com.ufh.ufhresidencemaintenance.util.PdfUploadValidator;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Controller
public class UserController {

    private static final Pattern STAFF_ID_PATTERN = Pattern.compile("^UFH/[0-9]{3}$");
    private static final Pattern STUDENT_NUMBER_PATTERN = Pattern.compile("^[0-9]{9}$");
    private static final Pattern FULL_NAME_PATTERN =
            Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ .'-][A-Za-zÀ-ÖØ-öø-ÿ]+)*$");
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_LOCK_MINUTES = 15;
    private static final int MAX_RESET_ATTEMPTS = 3;
    private static final int RESET_WINDOW_MINUTES = 30;

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
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam UserType userType,
            @RequestParam(required = false) Long residenceId,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) String studentNumber,
            @RequestParam(required = false) MultipartFile proofDocument,
            Model model) throws IOException {

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return registerError(model, "An account with this email already exists. Please sign in or use another email address.");
        }
        String normalizedFullName = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        if (!FULL_NAME_PATTERN.matcher(normalizedFullName).matches()) {
            return registerError(model, "Full name may contain letters, spaces, apostrophes, hyphens and full stops only.");
        }
        if (password == null || password.length() < 8) {
            return registerError(model, "Password must contain at least 8 characters.");
        }

        User newUser = new User();
        newUser.setFullName(normalizedFullName);
        newUser.setEmail(normalizedEmail);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setUserType(userType);

        if (userType == UserType.STUDENT) {
            if (proofDocument == null || proofDocument.isEmpty()) {
                return registerError(model, "Proof of residence registration is required for student accounts. Please upload the document before creating your account.");
            }
            if (!PdfUploadValidator.isValid(proofDocument)) {
                return registerError(model, "Proof of residence registration must be a valid PDF file no larger than 5MB.");
            }

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
                String fileName = UUID.randomUUID() + "_proof_of_residence.pdf";
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

    @PostMapping("/verification/reupload")
    public String reuploadVerification(@RequestParam MultipartFile proofDocument,
                                       HttpSession session, Model model) throws IOException {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";

        User student = userRepository.findById(userId).orElse(null);
        if (student == null || student.getUserType() != UserType.STUDENT) return "redirect:/login";
        if (student.getVerificationStatus() != VerificationStatus.REJECTED) {
            return "redirect:/dashboard";
        }
        if (!PdfUploadValidator.isValid(proofDocument)) {
            model.addAttribute("user", student);
            ResidenceZone activeZone = residenceZoneRepository.findByActiveTrue().orElse(null);
            model.addAttribute("activeZone", activeZone);
            model.addAttribute("bulletins", bulletinRepository.findTop3ByActiveTrueOrderByCreatedAtDesc());
            model.addAttribute("verificationUploadError", "Please upload a valid PDF file no larger than 5MB.");
            return "dashboard";
        }

        String uploadDir = "uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String oldPath = student.getProofDocumentPath();
        String fileName = UUID.randomUUID() + "_proof_of_residence.pdf";
        Path filePath = Paths.get(uploadDir + fileName);
        Files.write(filePath, proofDocument.getBytes());

        if (oldPath != null) {
            try { Files.deleteIfExists(Paths.get(oldPath)); } catch (Exception ignored) { }
        }

        student.setProofDocumentPath(filePath.toString());
        student.setVerificationStatus(VerificationStatus.PENDING);
        student.setVerificationReviewedAt(null);
        student.setVerificationReviewedByAdmin(null);
        student.setVerificationReviewMessage("Your new proof of residence document has been submitted and is awaiting administrator review.");
        userRepository.save(student);
        return "redirect:/dashboard?verificationResubmitted=true";
    }

    @GetMapping("/login")
    public String showLoginForm() { return "login"; }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email, @RequestParam String password,
                            Model model, HttpSession session) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Invalid email or password.");
            return "login";
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            model.addAttribute("error", "Too many incorrect attempts. Try again in " + minutes +
                    " minute(s), or use Forgot password.");
            return "login";
        }

        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (passwordEncoder.matches(password, user.getPassword())) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            session.setAttribute("loggedInUser", user.getId());
            if (user.getUserType() == UserType.ADMIN) return "redirect:/admin/dashboard";
            return "redirect:/dashboard";
        }

        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (LoginSecurityPolicy.shouldLock(attempts)) {
            user.setLockedUntil(now.plusMinutes(LOGIN_LOCK_MINUTES));
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            model.addAttribute("error", "Too many incorrect attempts. Your account is temporarily locked for 15 minutes. You can use Forgot password instead.");
        } else {
            userRepository.save(user);
            model.addAttribute("error", "Invalid email or password. " + (MAX_LOGIN_ATTEMPTS - attempts) +
                    " attempt(s) remaining before a temporary lock.");
        }
        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email,
                                 @RequestParam String identifier,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedIdentifier = identifier == null ? "" : identifier.trim();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (newPassword == null || newPassword.length() < 8) {
            model.addAttribute("error", "New password must contain at least 8 characters.");
            return "forgot-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "The new passwords do not match.");
            return "forgot-password";
        }
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "The details could not be verified. Please check them and try again.");
            return "forgot-password";
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();
        if (user.getPasswordResetWindowStart() == null ||
                Duration.between(user.getPasswordResetWindowStart(), now).toMinutes() >= RESET_WINDOW_MINUTES) {
            user.setPasswordResetWindowStart(now);
            user.setPasswordResetAttempts(0);
        }
        if (user.getPasswordResetAttempts() >= MAX_RESET_ATTEMPTS) {
            long minutes = Math.max(1, RESET_WINDOW_MINUTES -
                    Duration.between(user.getPasswordResetWindowStart(), now).toMinutes());
            model.addAttribute("error", "Too many reset attempts. Please wait " + minutes + " minute(s) before trying again.");
            return "forgot-password";
        }

        boolean identityMatches;
        if (user.getUserType() == UserType.STUDENT) {
            identityMatches = user.getStudentNumber() != null && user.getStudentNumber().equals(normalizedIdentifier);
        } else {
            identityMatches = user.getStaffId() != null && user.getStaffId().equalsIgnoreCase(normalizedIdentifier);
        }

        if (!identityMatches) {
            user.setPasswordResetAttempts(user.getPasswordResetAttempts() + 1);
            userRepository.save(user);
            int remaining = MAX_RESET_ATTEMPTS - user.getPasswordResetAttempts();
            model.addAttribute("error", "The details could not be verified. " + remaining +
                    " reset attempt(s) remaining in this window.");
            return "forgot-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setPasswordResetAttempts(0);
        user.setPasswordResetWindowStart(null);
        userRepository.save(user);
        model.addAttribute("success", "Your password has been changed successfully. You can now sign in with your new password.");
        return "forgot-password";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
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

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) return "redirect:/login";
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("user", currentUser);
        ResidenceZone activeZone = residenceZoneRepository.findByActiveTrue().orElse(null);
        model.addAttribute("activeZone", activeZone);
        model.addAttribute("bulletins", bulletinRepository.findTop3ByActiveTrueOrderByCreatedAtDesc());
        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) { session.invalidate(); return "redirect:/login?loggedout=true"; }
}
