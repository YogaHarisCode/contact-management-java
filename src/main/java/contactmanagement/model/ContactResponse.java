package contactmanagement.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public representation of a contact returned by the API, carried in the {@code data} field of
 * {@link WebResponse}.
 *
 * <p>Deliberately contains no owner information, so no data of the {@link contactmanagement.entity.User}
 * can leak through a contact response. Fields that are {@code null} are omitted from the JSON.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactResponse {

    /** Unique identifier (UUID string) of the contact. */
    private String id;

    /** First name of the contact. */
    private String firstName;

    /** Last name of the contact; {@code null} when not provided (omitted from the JSON). */
    private String lastName;

    /** Email address of the contact; {@code null} when not provided (omitted from the JSON). */
    private String email;

    /** Phone number of the contact; {@code null} when not provided (omitted from the JSON). */
    private String phone;
}