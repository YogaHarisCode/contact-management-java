package contactmanagement.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body of {@code POST /api/contacts} used to create a contact for the authenticated user.
 *
 * <p>Validated with Bean Validation when the controller parameter is annotated with
 * {@code @Valid}. Only {@code firstName} is required; a {@code null} optional field passes
 * validation and is stored as {@code null}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateContactRequest {

    /** First name of the contact. Required, not blank, maximum 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String firstName;

    /** Last name of the contact. Optional, maximum 100 characters. */
    @Size(max = 100)
    private String lastName;

    /** Email address of the contact. Optional, must be a valid email format, maximum 200 characters. */
    @Email
    @Size(max = 200)
    private String email;

    /**
     * Phone number of the contact. Optional; when present it must be an Indonesian mobile number
     * starting with {@code +62}, {@code 62}, or {@code 0}, followed by {@code 8}, a digit from 1
     * to 9, and 7 to 10 more digits (no spaces or dashes). The pattern also limits the length to
     * 15 characters, so no separate {@code @Size} is needed.
     */
    @Pattern(regexp = "^(\\+62|62|0)8[1-9][0-9]{7,10}$",
            message = "must be a valid Indonesian mobile number")
    private String phone;
}