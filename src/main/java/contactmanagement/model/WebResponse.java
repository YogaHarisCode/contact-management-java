package contactmanagement.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic envelope used by every API response.
 *
 * <p>A successful response carries {@code data} and no {@code errors}; a failed response carries
 * {@code errors} and no {@code data}. Fields that are {@code null} are omitted from the JSON.
 *
 * @param <T> the type of the payload held in {@code data}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebResponse <T> {

    /** Payload of a successful response; {@code null} when the request failed. */
    private T data;

    /** Error message of a failed response (currently always a {@code String}); {@code null} on success. */
    private Object errors;
}