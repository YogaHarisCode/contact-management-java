package com.yogaharis.contactmanagement.dto;

import com.yogaharis.contactmanagement.constraint.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"password"})
public class RegisterUserRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "must not contain whitespace")
    @Size(max = 100)
    private String username;

    @NotBlank
    @MaxBytes(72)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String name;
}
