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
import jakarta.servlet.http.HttpSession;
import com.ufh.ufhresidencemaintenance.model.Residence;
import com.ufh.ufhresidencemaintenance.repository.ResidenceRepository;
import com.ufh.ufhresidencemaintenance.repository.ResidenceZoneRepository;
import com.ufh.ufhresidencemaintenance.model.ResidenceZone;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResidenceRepository residenceRepository;

    @Autowired
    private ResidenceZoneRepository residenceZoneRepository;

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
            @RequestParam(required = false) MultipartFile proofDocument) throws IOException {

        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setUserType(userType);

        if (userType == UserType.STUDENT) {
            Residence residence = residenceRepository.findById(residenceId).orElse(null);
            newUser.setResidence(residence);
            newUser.setStudentNumber(studentNumber);
            newUser.setVerificationStatus(VerificationStatus.PENDING);

            if (proofDocument != null && !proofDocument.isEmpty()) {
                String uploadDir = "uploads/";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String fileName = UUID.randomUUID() + "_" + proofDocument.getOriginalFilename();
                Path filePath = Paths.get(uploadDir + fileName);
                Files.write(filePath, proofDocument.getBytes());
                newUser.setProofDocumentPath(filePath.toString());
            }
        } else if (userType == UserType.STAFF) {
            newUser.setStaffId(staffId);
        }

        userRepository.save(newUser);

        return "register-success";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            session.setAttribute("loggedInUser", userOpt.get().getId());

            if (userOpt.get().getUserType() == UserType.ADMIN) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");

        if (userId == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findById(userId).orElse(null);
        model.addAttribute("user", currentUser);
        ResidenceZone activeZone = residenceZoneRepository.findByActiveTrue().orElse(null);
        model.addAttribute("activeZone", activeZone);

        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?loggedout=true";
    }
}