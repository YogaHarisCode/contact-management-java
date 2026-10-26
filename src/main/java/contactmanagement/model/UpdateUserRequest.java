package contactmanagement.model;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "password")
public class UpdateUserRequest {

    // null means "keep the current value"; when present it must contain a non-whitespace character.
    @Size(max = 100)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String name;

    @Size(min = 8, max = 100)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String password;
}