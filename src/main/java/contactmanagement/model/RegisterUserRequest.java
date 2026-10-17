package contactmanagement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "^\\S+$", message = "must not contain whitespace") String username,
        @NotBlank @Size(max = 100, min = 8) String password,
        @NotBlank @Size(max = 100) String name
) {
}
