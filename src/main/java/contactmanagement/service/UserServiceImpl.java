package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UserResponse;
import contactmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USERNAME_ALREADY_EXISTS = "Username already exists";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse register(RegisterUserRequest request) {
        if (userRepository.existsById(request.getUsername())) {
            throw usernameAlreadyExists();
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw usernameAlreadyExists();
        }

        return toUserResponse(user);
    }

    private ResponseStatusException usernameAlreadyExists() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, USERNAME_ALREADY_EXISTS);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .build();
    }
}