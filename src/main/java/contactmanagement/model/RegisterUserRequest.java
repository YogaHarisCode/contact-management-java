package contactmanagement.model;

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
    @Size(max = 100)
    @Pattern(regexp = "^\\S+$", message = "must not contain whitespace")
    private String username;

    @NotBlank
    @Size(max = 100, min = 8)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String name;
}
