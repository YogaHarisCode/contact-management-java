package com.yogaharis.contactmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data returned after a successful login.
 *
 * @author Yoga Harisistianugroho
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenResponse {

    /** The new API token (a random UUID string), sent by clients in the {@code X-API-TOKEN} header. */
    private String token;

    /** Expiry time of the token as epoch milliseconds. */
    private Long tokenExpiredAt;
}