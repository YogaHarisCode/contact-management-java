package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Username or password wrong";
    private static final Duration TOKEN_TTL = Duration.ofDays(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String dummyPasswordHash;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public LoginResponse login(LoginUserRequest request) {
        String username = request.getUsername();
        log.debug("Login started: username={}", username);

        User user = userRepository.findById(username).orElse(null);

        String hashToCheck = (user != null) ? user.getPassword() : dummyPasswordHash;
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), hashToCheck);

        if (user == null || !passwordMatches) {
            // The reason is logged for operators only; the client always gets the same message.
            log.warn("Login rejected, invalid credentials: username={}, reason={}",
                    username, (user == null) ? "user_not_found" : "wrong_password");
            throw invalidCredentials();
        }

        user.setToken(UUID.randomUUID().toString());
        user.setTokenExpiredAt(Instant.now().plus(TOKEN_TTL).toEpochMilli());

        // The token itself is deliberately never logged.
        log.info("User logged in: username={}, tokenExpiredAt={}", username, user.getTokenExpiredAt());

        return LoginResponse.builder()
                .token(user.getToken())
                .tokenExpiredAt(user.getTokenExpiredAt())
                .build();
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
    }
}