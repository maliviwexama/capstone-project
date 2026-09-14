package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.Complaint;
import com.ufh.ufhresidencemaintenance.model.ComplaintCategory;
import com.ufh.ufhresidencemaintenance.model.ComplaintStatus;
import com.ufh.ufhresidencemaintenance.model.User;
import com.ufh.ufhresidencemaintenance.repository.ComplaintRepository;
import com.ufh.ufhresidencemaintenance.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import com.ufh.ufhresidencemaintenance.model.UserType;

import java.time.LocalDateTime;

@Controller
public class ComplaintController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/report-problem")
    public String showReportForm(HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }
        return "report-problem";
    }

    @PostMapping("/report-problem")
    public String submitProblem(
            @RequestParam String description,
            @RequestParam ComplaintCategory category,
            @RequestParam String roomNumber,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findById(userId).orElse(null);

        boolean isStaffOrAdmin = currentUser.getUserType() != UserType.STUDENT;
        boolean isHighSeverity = category.getSeverityWeight() >= 4;
        boolean residenceIsActive = currentUser.getResidence() != null
                && currentUser.getResidence().getZone() != null
                && currentUser.getResidence().getZone().isActive();

        if (!isStaffOrAdmin && !isHighSeverity && !residenceIsActive) {
            return "residence-not-active";
        }

        Complaint complaint = new Complaint();
        complaint.setDescription(description);
        complaint.setRoomNumber(roomNumber);
        complaint.setCategory(category);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setDateLogged(LocalDateTime.now());
        complaint.setUser(currentUser);

        complaintRepository.save(complaint);

        return "report-success";
    }
    @GetMapping("/my-requests")
    public String showMyRequests(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("complaints", complaintRepository.findByUserId(userId));
        return "my-requests";
    }
}
