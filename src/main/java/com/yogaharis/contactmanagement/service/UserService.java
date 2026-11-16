package com.yogaharis.contactmanagement.service;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
import com.yogaharis.contactmanagement.dto.UserResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private static final String USERNAME_ALREADY_EXISTS = "Username already exists";

    @Transactional
    public UserResponse register(RegisterUserRequest request){
        boolean exists = userRepository.existsById(request.getUsername());

        if (exists){
            throw responseStatusException(HttpStatus.CONFLICT, USERNAME_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());


        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e){
            throw responseStatusException(HttpStatus.CONFLICT, USERNAME_ALREADY_EXISTS);
        }

        return toUserResponse(user);
    }

    private ResponseStatusException responseStatusException(HttpStatusCode statusCode, String message){
        return new ResponseStatusException(statusCode, message);
    }

    private UserResponse toUserResponse(User user){
        return UserResponse.builder()
                .username(user.getUsername()).name(user.getName()).build();
    }
}
