package com.yogaharis.contactmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public representation of a user returned by the API.
 *
 * <p>It deliberately has no password field, so credentials can never be exposed
 * through a response.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    /** Unique login name of the user. */
    private String username;

    /** Display name of the user. */
    private String name;
}