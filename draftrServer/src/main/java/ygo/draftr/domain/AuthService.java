package ygo.draftr.domain;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ygo.draftr.controllers.dto.AuthResponse;
import ygo.draftr.data.UserRepository;
import ygo.draftr.models.User;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository userRepo, PasswordEncoder encoder, JwtService jwt) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(String username, String email, String password) {

        String u = username == null ? "" : username.trim();
        String e = email == null ? "" : email.trim().toLowerCase();

        if (u.length() < 3)
            throw new IllegalArgumentException("username must be at least 3 chars");

        if (!e.contains("@"))
            throw new IllegalArgumentException("invalid email");

        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("password must be at least 8 chars");

        if (userRepo.existsByUsernameIgnoreCase(u)) {
            throw new IllegalArgumentException("username already taken");
        }

        if (userRepo.existsByEmailIgnoreCase(e)) {
            throw new IllegalArgumentException("email already taken");
        }

        User user = new User();
        user.setUsername(u);
        user.setEmail(e); // ✅ ADD THIS
        user.setPasswordHash(encoder.encode(password));

        User saved = userRepo.save(user);

        AuthResponse resp = new AuthResponse();
        resp.setUserId(saved.getUserId());
        resp.setUsername(saved.getUsername());
        resp.setToken(jwt.createToken(saved.getUserId(), saved.getUsername()));

        return resp;
    }

    public AuthResponse login(String username, String password) {
        User user = userRepo.findByUsernameIgnoreCase(username == null ? "" : username.trim())
                .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        AuthResponse resp = new AuthResponse();
        resp.setUserId(user.getUserId());
        resp.setUsername(user.getUsername());
        resp.setToken(jwt.createToken(user.getUserId(), user.getUsername()));
        return resp;
    }
}
