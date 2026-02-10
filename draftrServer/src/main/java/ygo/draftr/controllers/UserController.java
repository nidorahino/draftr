package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.data.UserRepository;
import ygo.draftr.models.User;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepo;

    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // GET /api/users/me
    @GetMapping("/me")
    public ResponseEntity<?> me() {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Long userId = (Long) auth.getPrincipal();

        User u = userRepo.findById(userId).orElse(null);

        if (u == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(u);
    }
}
