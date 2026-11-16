package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
import com.yogaharis.contactmanagement.dto.UserResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import com.yogaharis.contactmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> register(@Valid @RequestBody RegisterUserRequest request){
        UserResponse response = userService.register(request);
        return WebResponse.<UserResponse>builder().data(response).build();
    }
}
