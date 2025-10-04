package com.example.todo.controller;

import com.example.todo.model.user;
import com.example.todo.repository.userRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final userRepository userRepo;

    public AuthController(userRepository userRepo) {
        this.userRepo = userRepo;
    }

    // --- Register (Show GET form) ---
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new user());
        return "register"; // src/main/resources/templates/register.html
    }

    // --- Register (POST) ---
    @PostMapping("/register")
    public String register(@ModelAttribute user newUser, RedirectAttributes ra) {
        if (userRepo.existsByUsername(newUser.getUsername())) {
            ra.addFlashAttribute("error", "Username already exists!");
            return "redirect:/auth/register";
        }

        // Password hashing will be added later
        userRepo.save(newUser);
        ra.addFlashAttribute("success", "Registration successful. You can now login.");
        return "redirect:/auth/login";
    }

    // --- Login (Show GET form) ---
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new user());
        return "login";
    }

    // --- Login (POST) ---
    @PostMapping("/login")
    public String login(@ModelAttribute user loginUser, RedirectAttributes ra) {
        Optional<user> existingUser = userRepo.findByUsername(loginUser.getUsername());

        if (existingUser.isPresent()) {
            user dbUser = existingUser.get();
            if (dbUser.getPassword().equals(loginUser.getPassword())) {
                // TODO: Implement session management
                ra.addFlashAttribute("success", "Login successful!");
                return "redirect:/";
            }
        }

        ra.addFlashAttribute("error", "Invalid username or password!");
        return "redirect:/auth/login";
    }
}
