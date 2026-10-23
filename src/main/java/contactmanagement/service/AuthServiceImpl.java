package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginUserRequest request) {
        User user = userRepository.findById(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong");
        }

        user.setToken(UUID.randomUUID().toString());
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(30)).toEpochMilli());
        userRepository.save(user);

        return LoginResponse.builder().token(user.getToken()).tokenExpiredAt(user.getTokenExpiredAt()).build();
    }
}
