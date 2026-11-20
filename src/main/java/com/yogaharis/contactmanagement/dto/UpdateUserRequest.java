package com.yogaharis.contactmanagement.dto;

import com.yogaharis.contactmanagement.constraint.MaxBytes;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "password")
public class UpdateUserRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(min = 1, max = 100)
    private String name;

    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(min = 8)
    @MaxBytes(72)
    private String password;
}
