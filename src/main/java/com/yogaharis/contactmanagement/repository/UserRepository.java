package com.yogaharis.contactmanagement.repository;

import com.yogaharis.contactmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>The entity ID is the username, so lookups such as {@code existsById} and
 * {@code findById} take a username. Authenticated requests are looked up by their
 * API token with {@link #findByToken(String)}.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Finds the user that currently owns the given API token.
     *
     * <p>The {@code token} column is unique, so at most one user can match.
     * This method does not check whether the token has expired.</p>
     *
     * @param token the API token, as sent in the {@code X-API-TOKEN} header
     * @return the user owning the token, or an empty {@link Optional} if no user has it
     */
    Optional<User> findByToken(String token);
}