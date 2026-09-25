package com.ufh.ufhresidencemaintenance.controller;

import com.ufh.ufhresidencemaintenance.model.Bulletin;
import com.ufh.ufhresidencemaintenance.model.User;
import com.ufh.ufhresidencemaintenance.model.UserType;
import com.ufh.ufhresidencemaintenance.repository.BulletinRepository;
import com.ufh.ufhresidencemaintenance.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
public class BulletinController {
    @Autowired private BulletinRepository bulletinRepository;
    @Autowired private UserRepository userRepository;

    private User admin(HttpSession session) {
        Long id = (Long) session.getAttribute("loggedInUser");
        if (id == null) return null;
        User u = userRepository.findById(id).orElse(null);
        return u != null && u.getUserType() == UserType.ADMIN ? u : null;
    }

    @GetMapping("/updates")
    public String updates(HttpSession session, Model model) {
        Long id = (Long) session.getAttribute("loggedInUser");
        if (id == null) return "redirect:/login";
        model.addAttribute("updates", bulletinRepository.findByActiveTrueOrderByCreatedAtDesc());
        return "updates";
    }

    @GetMapping("/admin/bulletin")
    public String adminBulletin(HttpSession session, Model model) {
        if (admin(session) == null) return "redirect:/dashboard";
        model.addAttribute("bulletins", bulletinRepository.findAllByOrderByCreatedAtDesc());
        return "admin-bulletin";
    }

    @PostMapping("/admin/bulletin")
    public String publish(@RequestParam String title, @RequestParam String message,
                          HttpSession session) {
        User current = admin(session);
        if (current == null) return "redirect:/dashboard";
        Bulletin bulletin = new Bulletin();
        bulletin.setTitle(title);
        bulletin.setMessage(message);
        bulletin.setCreatedAt(LocalDateTime.now());
        bulletin.setCreatedBy(current);
        bulletin.setActive(true);
        bulletinRepository.save(bulletin);
        return "redirect:/admin/bulletin";
    }

    @PostMapping("/admin/bulletin/{id}/archive")
    public String archive(@PathVariable Long id, HttpSession session) {
        if (admin(session) == null) return "redirect:/dashboard";
        bulletinRepository.findById(id).ifPresent(b -> {
            b.setActive(false);
            bulletinRepository.save(b);
        });
        return "redirect:/admin/bulletin";
    }
}
