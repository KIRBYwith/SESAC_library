package com.example.sesac_library.controller;

import com.example.sesac_library.dto.LoginRequest;
import com.example.sesac_library.dto.RegisterRequest;
import com.example.sesac_library.entity.User;
import com.example.sesac_library.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    /* 회원가입 */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            User user = userService.registerUser(request.getUsername(), request.getPassword(),
                request.getEmail(), request.getPhone(), request.getAddr());
            response.put("message", "User registered successfully");
            response.put("username", user.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    /* 로그인 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request, HttpSession session) {
        Map<String, String> response = new HashMap<>();

        if (userService.authenticateUser(request.getUsername(), request.getPassword())) {
            session.setAttribute("username", request.getUsername());
            response.put("message", "Login successful");
            response.put("username", request.getUsername());
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(response);
        }
    }


    /* 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        Map<String, String> response = new HashMap<>();
        session.invalidate();
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
    }


    /* 현재 로그인 세션 사용자 정보 */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("username");

        if (username != null) {
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put("authenticated", true);
                response.put("userId", user.getUserId());
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("phone", user.getPhone());
                response.put("addr", user.getAddr());
                response.put("points", user.getPoints());
                response.put("role", user.getRole().toString());
                return ResponseEntity.ok(response);
            }
        }

        response.put("authenticated", false);
        return ResponseEntity.status(401).body(response);
    }
}