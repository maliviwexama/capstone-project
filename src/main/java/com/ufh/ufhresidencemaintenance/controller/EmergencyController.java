package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.User;
import com.ufh.ufhresidencemaintenance.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EmergencyController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/emergency")
    public String emergency(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUser");
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        model.addAttribute("residenceManagerPhone",
                user != null && user.getResidence() != null ? user.getResidence().getManagerPhone() : null);

        return "emergency";
    }
}
