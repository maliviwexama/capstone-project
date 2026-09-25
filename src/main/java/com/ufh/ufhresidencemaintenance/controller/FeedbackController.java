package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.*;
import com.ufh.ufhresidencemaintenance.repository.ComplaintRepository;
import com.ufh.ufhresidencemaintenance.repository.FeedbackRepository;
import com.ufh.ufhresidencemaintenance.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;


    @GetMapping("/feedback")
    public String showFeedbackForm(@RequestParam Long complaintId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);

        if (complaint == null
                || complaint.getUser() == null
                || !complaint.getUser().getId().equals(userId)
                || complaint.getStatus() != ComplaintStatus.RESOLVED) {
            return "redirect:/my-requests";
        }

        if (feedbackRepository.existsByComplaintId(complaintId)) {
            return "redirect:/my-requests";
        }

        model.addAttribute("complaint", complaint);
        return "feedback-form";
    }

    // Submit feedback
    @PostMapping("/feedback")
    public String submitFeedback(
            @RequestParam Long complaintId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String comment,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("loggedInUser");
        if (userId == null) {
            return "redirect:/login";
        }

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);

        if (complaint == null
                || complaint.getUser() == null
                || !complaint.getUser().getId().equals(userId)
                || complaint.getStatus() != ComplaintStatus.RESOLVED
                || feedbackRepository.existsByComplaintId(complaintId)
                || (rating != null && (rating < 1 || rating > 5))) {
            return "redirect:/my-requests";
        }

        User currentUser = userRepository.findById(userId).orElse(null);

        Feedback feedback = new Feedback();
        feedback.setComplaint(complaint);
        feedback.setUser(currentUser);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setDateSubmitted(LocalDateTime.now());

        feedbackRepository.save(feedback);

        return "redirect:/my-requests";
    }

}