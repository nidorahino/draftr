package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ygo.draftr.controllers.dto.*;
import ygo.draftr.data.UserRepository;
import ygo.draftr.domain.AuthService;
import ygo.draftr.domain.EmailService;
import ygo.draftr.domain.JwtService;
import ygo.draftr.models.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthController(UserRepository userRepo,
                          PasswordEncoder encoder,
                          JwtService jwtService,
                          EmailService emailService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        if (userRepo.existsByUsernameIgnoreCase(req.getUsername())) {
            return ResponseEntity.badRequest()
                    .body("Username already exists");
        }

        if (userRepo.existsByEmailIgnoreCase(req.getEmail())) {
            return ResponseEntity.badRequest()
                    .body("Email already exists");
        }

        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail()); // ✅ ADD THIS
        u.setPasswordHash(encoder.encode(req.getPassword()));

        userRepo.save(u);

        String token = jwtService.createToken(u.getUserId(), u.getUsername());

        AuthResponse resp = new AuthResponse();
        resp.setUserId(u.getUserId());
        resp.setUsername(u.getUsername());
        resp.setToken(token);

        return ResponseEntity.ok(resp);
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        User u = userRepo.findByUsernameIgnoreCase(req.getUsername())
                .orElse(null);

        if (u == null ||
                !encoder.matches(req.getPassword(), u.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtService.createToken(u.getUserId(), u.getUsername());

        AuthResponse resp = new AuthResponse();
        resp.setUserId(u.getUserId());
        resp.setUsername(u.getUsername());
        resp.setToken(token);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {

        String email = req.getEmail() == null ? "" : req.getEmail().trim();

        // Always return 200 to avoid leaking whether an email exists
        User user = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok().build();
        }

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(Instant.now().plus(15, ChronoUnit.MINUTES));

        userRepo.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), token);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {

        String token = req.getToken() == null ? "" : req.getToken().trim();
        String newPassword = req.getNewPassword();

        if (token.isBlank()) {
            return ResponseEntity.badRequest().body("Missing token");
        }

        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body("password must be at least 8 chars");
        }

        User user = userRepo.findByResetToken(token).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid token");
        }

        Instant expiry = user.getResetTokenExpiry();
        if (expiry == null || expiry.isBefore(Instant.now())) {
            return ResponseEntity.badRequest().body("Token expired");
        }

        user.setPasswordHash(encoder.encode(newPassword));

        // Invalidate token after use
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepo.save(user);

        return ResponseEntity.ok().build();
    }

}
